package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;

/**
 * 校長向けの全校安否確認データを取得するDAO。
 */
public class PrincipalSafetyDao extends DAO {

    public List<ClassSummary> findClassesByGrade(int grade)
            throws NamingException, SQLException {

        List<ClassSummary> classes = new ArrayList<>();

        String sql = """
                SELECT
                    c.id AS class_id,
                    c.school_year,
                    c.class_name,
                    COUNT(u.id) AS student_count,
                    COUNT(latest_report.id) AS answered_count,
                    COUNT(u.id) - COUNT(latest_report.id) AS unanswered_count,
                    COUNT(latest_report.id) FILTER (
                        WHERE latest_report.status IN ('DANGER', 'WORRIED')
                    ) AS attention_count
                FROM public.school_class AS c
                LEFT JOIN public.student_class AS sc
                  ON sc.class_id = c.id
                 AND sc.school_year = c.school_year
                LEFT JOIN public.users AS u
                  ON u.id = sc.student_user_id
                 AND u.role = 'STUDENT'
                 AND u.active = TRUE
                LEFT JOIN LATERAL (
                    SELECT sr.id, sr.status, sr.danger_type, sr.reported_at
                    FROM public.safety_report AS sr
                    WHERE sr.user_id = u.id
                      AND NOT EXISTS (
                          SELECT 1
                          FROM public.safety_report_cancellation AS cancel
                          WHERE cancel.report_id = sr.id
                      )
                    ORDER BY sr.reported_at DESC, sr.id DESC
                    LIMIT 1
                ) AS latest_report ON TRUE
                WHERE c.school_year = (
                    SELECT MAX(school_year)
                    FROM public.school_class
                )
                  AND c.class_name LIKE ?
                GROUP BY c.id, c.school_year, c.class_name
                ORDER BY c.class_name, c.id
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, grade + "年%");

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ClassSummary summary = new ClassSummary();
                    summary.setClassId(resultSet.getLong("class_id"));
                    summary.setSchoolYear(resultSet.getInt("school_year"));
                    summary.setClassName(resultSet.getString("class_name"));
                    summary.setStudentCount(resultSet.getInt("student_count"));
                    summary.setAnsweredCount(resultSet.getInt("answered_count"));
                    summary.setUnansweredCount(resultSet.getInt("unanswered_count"));
                    summary.setAttentionCount(resultSet.getInt("attention_count"));
                    classes.add(summary);
                }
            }
        }

        return classes;
    }

    /**
     * 指定クラスの生徒と最新安否回答を取得する。
     */
    public ClassStudentsResult findStudentsByClass(
            long classId,
            int schoolYear)
            throws NamingException, SQLException {

        String classSql = """
                SELECT id, school_year, class_name
                FROM public.school_class
                WHERE id = ?
                  AND school_year = ?
                """;

        ClassStudentsResult result;

        try (Connection connection = getConnection();
             PreparedStatement classStatement = connection.prepareStatement(classSql)) {

            classStatement.setLong(1, classId);
            classStatement.setInt(2, schoolYear);

            try (ResultSet rs = classStatement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                result = new ClassStudentsResult();
                result.setClassId(rs.getLong("id"));
                result.setSchoolYear(rs.getInt("school_year"));
                result.setClassName(rs.getString("class_name"));
                result.setGrade(extractGrade(result.getClassName()));
            }

            String studentSql = """
                    SELECT
                        u.id AS student_user_id,
                        u.login_id,
                        u.name,
                        latest_report.id AS report_id,
                        latest_report.status,
                        latest_report.danger_type,
                        latest_report.reported_at
                    FROM public.student_class AS sc
                    JOIN public.users AS u
                      ON u.id = sc.student_user_id
                     AND u.role = 'STUDENT'
                     AND u.active = TRUE
                    LEFT JOIN LATERAL (
                        SELECT sr.id, sr.status, sr.danger_type, sr.reported_at
                        FROM public.safety_report AS sr
                        WHERE sr.user_id = u.id
                          AND NOT EXISTS (
                              SELECT 1
                              FROM public.safety_report_cancellation AS cancel
                              WHERE cancel.report_id = sr.id
                          )
                        ORDER BY sr.reported_at DESC, sr.id DESC
                        LIMIT 1
                    ) AS latest_report ON TRUE
                    WHERE sc.class_id = ?
                      AND sc.school_year = ?
                    ORDER BY u.login_id, u.id
                    """;

            try (PreparedStatement studentStatement = connection.prepareStatement(studentSql)) {
                studentStatement.setLong(1, classId);
                studentStatement.setInt(2, schoolYear);

                try (ResultSet rs = studentStatement.executeQuery()) {
                    while (rs.next()) {
                        StudentSummary student = new StudentSummary();
                        student.setStudentUserId(rs.getLong("student_user_id"));
                        student.setLoginId(rs.getString("login_id"));
                        student.setName(rs.getString("name"));

                        long reportId = rs.getLong("report_id");
                        if (!rs.wasNull()) {
                            student.setReportId(reportId);
                        }

                        student.setStatus(rs.getString("status"));
                        student.setDangerType(rs.getString("danger_type"));
                        student.setStatusClass(statusClass(student.getStatus()));
                        student.setStatusLabel(statusLabel(
                                student.getStatus(), student.getDangerType()));

                        result.getStudents().add(student);
                    }
                }
            }
        }

        int answered = 0;
        int attention = 0;

        for (StudentSummary student : result.getStudents()) {
            if (student.getReportId() != null) {
                answered++;
            }

            if ("DANGER".equals(student.getStatus())
                    || "WORRIED".equals(student.getStatus())) {
                attention++;
            }
        }

        result.setStudentCount(result.getStudents().size());
        result.setAnsweredCount(answered);
        result.setUnansweredCount(result.getStudentCount() - answered);
        result.setAttentionCount(attention);

        return result;
    }

    private int extractGrade(String className) {
        if (className == null || className.isBlank()) {
            return 0;
        }

        char first = className.charAt(0);
        if (first >= '1' && first <= '3') {
            return first - '0';
        }

        return 0;
    }

    private String statusClass(String status) {
        if ("SAFE".equals(status)) {
            return "safe";
        }
        if ("DANGER".equals(status)) {
            return "danger";
        }
        if ("WORRIED".equals(status)) {
            return "worried";
        }
        return "unanswered";
    }

    private String statusLabel(String status, String dangerType) {
        if (status == null) {
            return "未回答";
        }
        if ("SAFE".equals(status)) {
            return "無事";
        }

        String typeLabel = dangerTypeLabel(dangerType);

        if ("DANGER".equals(status)) {
            return typeLabel == null ? "危険" : "危険（" + typeLabel + "）";
        }

        if ("WORRIED".equals(status)) {
            return typeLabel == null ? "困っている" : "困っている（" + typeLabel + "）";
        }

        return status;
    }

    private String dangerTypeLabel(String dangerType) {
        if (dangerType == null) {
            return null;
        }

        return switch (dangerType) {
        case "ACCIDENT" -> "事故";
        case "EARTHQUAKE" -> "地震";
        case "FIRE" -> "火災";
        case "LANDSLIDE" -> "土砂崩れ";
        case "HEATSTROKE" -> "熱中症";
        case "STALKING" -> "ストーカー";
        default -> dangerType;
        };
    }

    // ホームと回答詳細で共通の最新回答取得条件を使用する。
    private static final ZoneId JAPAN_ZONE = ZoneId.of("Asia/Tokyo");
    private static final DateTimeFormatter HOME_TIME =
            DateTimeFormatter.ofPattern("M/d HH:mm", Locale.JAPAN);
    private static final DateTimeFormatter DETAIL_TIME =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss", Locale.JAPAN);
    private static final DateTimeFormatter HOME_DATE =
            DateTimeFormatter.ofPattern("MM月dd日", Locale.JAPAN);

    /**
     * 有効な全生徒を取得する（未所属の生徒も含む）。
     * 最新年度の所属学級と、取消されていない最新回答を表示する。
     */
    private static final String SCHOOL_SAFETY_SQL = """
            SELECT
                u.id AS student_user_id,
                u.login_id,
                u.name,
                c.id AS class_id,
                c.school_year,
                c.class_name,
                latest.id AS report_id,
                latest.status,
                latest.danger_type,
                latest.reported_at,
                latest.latitude,
                latest.longitude,
                latest.comment,
                wt.name AS worried_type_name
            FROM public.users AS u
            LEFT JOIN public.student_class AS sc
              ON sc.student_user_id = u.id
             AND sc.school_year = (
                    SELECT MAX(school_year) FROM public.school_class
                 )
            LEFT JOIN public.school_class AS c
              ON c.id = sc.class_id
             AND c.school_year = sc.school_year
            LEFT JOIN LATERAL (
                SELECT sr.id, sr.status, sr.danger_type, sr.reported_at,
                       sr.latitude, sr.longitude, sr.comment
                FROM public.safety_report AS sr
                WHERE sr.user_id = u.id
                  AND NOT EXISTS (
                      SELECT 1
                      FROM public.safety_report_cancellation AS cancelled
                      WHERE cancelled.report_id = sr.id
                  )
                ORDER BY sr.reported_at DESC, sr.id DESC
                LIMIT 1
            ) AS latest ON TRUE
            LEFT JOIN public.worried_type AS wt
              ON latest.status = 'WORRIED'
             AND wt.code = latest.danger_type
            WHERE u.role = 'STUDENT'
              AND u.active = TRUE
            """;

    /** 全校集計と警告一覧、最新3件の回答を1回のSELECTで取得する。 */
    public HomeData findHomeData() throws NamingException, SQLException {
        HomeData data = new HomeData();
        String sql = SCHOOL_SAFETY_SQL + """
                ORDER BY latest.reported_at DESC NULLS LAST,
                         latest.id DESC NULLS LAST, u.id
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                HomeStudent student = readHomeStudent(rs);
                data.studentCount++;

                if (student.getReportId() != null) {
                    data.answeredCount++;

                    // 各生徒の現在の回答を、送信日時の新しい順に表示する。
                    if (data.latestAnswers.size() < 3) {
                        data.latestAnswers.add(student);
                    }
                }

                if ("DANGER".equals(student.getStatus())
                        || "WORRIED".equals(student.getStatus())) {
                    data.attentionStudents.add(student);
                }
            }
        }
        return data;
    }

    /** 校長が指定した有効な生徒の最新回答詳細を取得する。 */
    public HomeStudent findStudentDetail(long studentId)
            throws NamingException, SQLException {

        String sql = SCHOOL_SAFETY_SQL + " AND u.id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? readHomeStudent(rs) : null;
            }
        }
    }

    private HomeStudent readHomeStudent(ResultSet rs) throws SQLException {
        HomeStudent student = new HomeStudent();
        student.setStudentUserId(rs.getLong("student_user_id"));
        student.setLoginId(rs.getString("login_id"));
        student.setName(rs.getString("name"));
        student.classId = rs.getObject("class_id", Long.class);
        student.schoolYear = rs.getObject("school_year", Integer.class);
        student.className = rs.getString("class_name");
        student.grade = extractGrade(student.className);
        student.setReportId(rs.getObject("report_id", Long.class));
        student.setStatus(rs.getString("status"));
        student.setDangerType(rs.getString("danger_type"));
        student.setStatusClass(statusClass(student.getStatus()));

        String registeredType = rs.getString("worried_type_name");
        if ("WORRIED".equals(student.getStatus()) && registeredType != null) {
            student.setStatusLabel("困っている（" + registeredType + "）");
        } else {
            student.setStatusLabel(statusLabel(student.getStatus(), student.getDangerType()));
        }

        student.reportedAt = rs.getObject("reported_at", OffsetDateTime.class);
        student.latitude = rs.getObject("latitude", Double.class);
        student.longitude = rs.getObject("longitude", Double.class);
        student.comment = rs.getString("comment");
        return student;
    }

    /** 管理ホーム画面の表示データ。 */
    public static class HomeData {
        private int studentCount;
        private int answeredCount;
        private final List<HomeStudent> attentionStudents = new ArrayList<>();
        private final List<HomeStudent> latestAnswers = new ArrayList<>();

        public int getStudentCount() { return studentCount; }
        public int getAnsweredCount() { return answeredCount; }
        public int getUnansweredCount() { return studentCount - answeredCount; }
        public int getAttentionCount() { return attentionStudents.size(); }
        public int getProgressPercent() {
            return studentCount == 0 ? 0
                    : (int) Math.round(100.0 * answeredCount / studentCount);
        }
        public String getDisplayDate() {
            return LocalDate.now(JAPAN_ZONE).format(HOME_DATE);
        }
        public String getLatestAnsweredAt() {
            return latestAnswers.isEmpty() ? "—" : latestAnswers.get(0).getReportedAtLabel();
        }
        public List<HomeStudent> getAttentionStudents() { return attentionStudents; }
        public List<HomeStudent> getLatestAnswers() { return latestAnswers; }
    }

    /** 管理ホーム・回答詳細用の生徒情報。 */
    public static class HomeStudent extends StudentSummary {
        private Long classId;
        private Integer schoolYear;
        private String className;
        private int grade;
        private OffsetDateTime reportedAt;
        private Double latitude;
        private Double longitude;
        private String comment;

        public Long getClassId() { return classId; }
        public Integer getSchoolYear() { return schoolYear; }
        public String getClassName() { return className == null ? "未所属" : className; }
        public int getGrade() { return grade; }
        public String getAvatar() {
            String name = getName();
            if (name == null || name.isBlank()) return "生";
            name = name.strip();
            return name.substring(0, name.offsetByCodePoints(0, 1));
        }
        public String getReportedAtLabel() {
            return reportedAt == null ? "—"
                    : reportedAt.atZoneSameInstant(JAPAN_ZONE).format(HOME_TIME);
        }
        public String getDetailTimeLabel() {
            return reportedAt == null ? "—"
                    : reportedAt.atZoneSameInstant(JAPAN_ZONE).format(DETAIL_TIME);
        }
        public Double getLatitude() { return latitude; }
        public Double getLongitude() { return longitude; }
        public boolean isLocationAvailable() { return latitude != null && longitude != null; }
        public String getLocationLabel() {
            return !isLocationAvailable() ? "位置情報なし"
                    : String.format(Locale.ROOT, "緯度 %.6f / 経度 %.6f", latitude, longitude);
        }
        public String getComment() { return comment; }
    }

    public static class ClassSummary {
        private long classId;
        private int schoolYear;
        private String className;
        private int studentCount;
        private int answeredCount;
        private int unansweredCount;
        private int attentionCount;

        public long getClassId() { return classId; }
        public void setClassId(long classId) { this.classId = classId; }
        public int getSchoolYear() { return schoolYear; }
        public void setSchoolYear(int schoolYear) { this.schoolYear = schoolYear; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        public int getStudentCount() { return studentCount; }
        public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
        public int getAnsweredCount() { return answeredCount; }
        public void setAnsweredCount(int answeredCount) { this.answeredCount = answeredCount; }
        public int getUnansweredCount() { return unansweredCount; }
        public void setUnansweredCount(int unansweredCount) { this.unansweredCount = unansweredCount; }
        public int getAttentionCount() { return attentionCount; }
        public void setAttentionCount(int attentionCount) { this.attentionCount = attentionCount; }
    }

    public static class ClassStudentsResult {
        private long classId;
        private int schoolYear;
        private String className;
        private int grade;
        private int studentCount;
        private int answeredCount;
        private int unansweredCount;
        private int attentionCount;
        private final List<StudentSummary> students = new ArrayList<>();

        public long getClassId() { return classId; }
        public void setClassId(long classId) { this.classId = classId; }
        public int getSchoolYear() { return schoolYear; }
        public void setSchoolYear(int schoolYear) { this.schoolYear = schoolYear; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        public int getGrade() { return grade; }
        public void setGrade(int grade) { this.grade = grade; }
        public int getStudentCount() { return studentCount; }
        public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
        public int getAnsweredCount() { return answeredCount; }
        public void setAnsweredCount(int answeredCount) { this.answeredCount = answeredCount; }
        public int getUnansweredCount() { return unansweredCount; }
        public void setUnansweredCount(int unansweredCount) { this.unansweredCount = unansweredCount; }
        public int getAttentionCount() { return attentionCount; }
        public void setAttentionCount(int attentionCount) { this.attentionCount = attentionCount; }
        public List<StudentSummary> getStudents() { return students; }
    }

    public static class StudentSummary {
        private long studentUserId;
        private String loginId;
        private String name;
        private Long reportId;
        private String status;
        private String dangerType;
        private String statusLabel;
        private String statusClass;

        public long getStudentUserId() { return studentUserId; }
        public void setStudentUserId(long studentUserId) { this.studentUserId = studentUserId; }
        public String getLoginId() { return loginId; }
        public void setLoginId(String loginId) { this.loginId = loginId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Long getReportId() { return reportId; }
        public void setReportId(Long reportId) { this.reportId = reportId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getDangerType() { return dangerType; }
        public void setDangerType(String dangerType) { this.dangerType = dangerType; }
        public String getStatusLabel() { return statusLabel; }
        public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
        public String getStatusClass() { return statusClass; }
        public void setStatusClass(String statusClass) { this.statusClass = statusClass; }
    }
}
