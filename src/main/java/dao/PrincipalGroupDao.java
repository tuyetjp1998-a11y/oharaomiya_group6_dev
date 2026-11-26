package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.naming.NamingException;

/** 校長向けグループ管理のデータを取得する。 */
public class PrincipalGroupDao extends DAO {
    public List<GroupSummary> findGroups() throws NamingException, SQLException {
        String sql = """
                SELECT c.id, c.school_year, c.class_name,
                       COALESCE((SELECT STRING_AGG(t.name, '、' ORDER BY t.id)
                           FROM public.teacher_class tc
                           JOIN public.users t ON t.id = tc.teacher_user_id
                           WHERE tc.class_id = c.id AND t.role = 'TEACHER'
                             AND t.active = TRUE), '職員未割り当て') AS teacher_name,
                       (SELECT COUNT(*) FROM public.student_class sc
                           JOIN public.users s ON s.id = sc.student_user_id
                           WHERE sc.class_id = c.id AND sc.school_year = c.school_year
                             AND s.role = 'STUDENT' AND s.active = TRUE) AS student_count
                FROM public.school_class c
                WHERE c.school_year = (SELECT MAX(school_year) FROM public.school_class)
                ORDER BY c.class_name, c.id
                """;
        List<GroupSummary> groups = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                groups.add(new GroupSummary(rs.getLong("id"), rs.getInt("school_year"),
                        rs.getString("class_name"), rs.getString("teacher_name"),
                        rs.getLong("student_count")));
            }
        }
        return groups;
    }

    /** 最新年度にクラスを作成する。同じ年度・クラス名は重複登録しない。 */
    public boolean createClass(String className) throws NamingException, SQLException {
        if (className == null || className.isBlank() || className.length() > 30
                || className.indexOf(0) >= 0) {
            throw new IllegalArgumentException("クラス名を1文字以上30文字以内で入力してください。");
        }
        String sql = """
                INSERT INTO public.school_class (school_year, class_name)
                SELECT COALESCE(MAX(school_year), ?), ?
                FROM public.school_class
                ON CONFLICT (school_year, class_name) DO NOTHING
                """;
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Tokyo"));
        int fallbackYear = today.getYear() - (today.getMonthValue() < 4 ? 1 : 0);
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, fallbackYear);
            statement.setString(2, className.strip());
            return statement.executeUpdate() == 1;
        }
    }

    public record TeacherOption(long id, String name, String loginId) {
        public long getId() { return id; }
        public String getName() { return name; }
        public String getLoginId() { return loginId; }
    }

    public List<TeacherOption> findTeachers() throws NamingException, SQLException {
        String sql = "SELECT id, name, login_id FROM public.users WHERE role = 'TEACHER' AND active = TRUE ORDER BY name, id";
        List<TeacherOption> result = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(new TeacherOption(rs.getLong(1), rs.getString(2), rs.getString(3)));
        }
        return result;
    }

    public Long findAssignedTeacher(long classId) throws NamingException, SQLException {
        String sql = """
                SELECT t.id FROM public.teacher_class tc JOIN public.users t ON t.id = tc.teacher_user_id
                WHERE tc.class_id = ? AND t.role = 'TEACHER' AND t.active = TRUE ORDER BY t.id
                """;
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                long id = rs.getLong(1);
                return rs.next() ? null : id;
            }
        }
    }

    /** 選択されたクラスの担当職員を置き換える。nullは未割り当て。 */
    public void assignTeacher(long classId, Long teacherId) throws NamingException, SQLException {
        if (classId <= 0 || (teacherId != null && teacherId <= 0)) throw new IllegalArgumentException("入力内容を確認してください。");
        try (Connection c = getConnection()) {
            boolean auto = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                // 同じクラスへの割り当てを直列化する。
                try (PreparedStatement ps = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                    ps.setLong(1, classId); ps.executeQuery().close();
                }
                try (PreparedStatement ps = c.prepareStatement("SELECT id FROM public.school_class WHERE id = ? AND school_year = (SELECT MAX(school_year) FROM public.school_class)")) {
                    ps.setLong(1, classId);
                    try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw new IllegalArgumentException("クラスを確認してください。"); }
                }
                if (teacherId != null) {
                    try (PreparedStatement ps = c.prepareStatement("SELECT id FROM public.users WHERE id = ? AND role = 'TEACHER' AND active = TRUE")) {
                        ps.setLong(1, teacherId);
                        try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw new IllegalArgumentException("職員を確認してください。"); }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM public.teacher_class WHERE class_id = ?")) {
                    ps.setLong(1, classId); ps.executeUpdate();
                }
                if (teacherId != null) {
                    try (PreparedStatement ps = c.prepareStatement("INSERT INTO public.teacher_class (teacher_user_id, class_id) VALUES (?, ?)")) {
                        ps.setLong(1, teacherId); ps.setLong(2, classId); ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); throw e; }
                c.setAutoCommit(auto); throw e;
            }
            c.setAutoCommit(auto);
        }
    }

    public static final class GroupSummary {
        private final long classId;
        private final int schoolYear;
        private final String className;
        private final String teacherName;
        private final long studentCount;
        public GroupSummary(long classId, int schoolYear, String className,
                            String teacherName, long studentCount) {
            this.classId = classId;
            this.schoolYear = schoolYear;
            this.className = className;
            this.teacherName = teacherName;
            this.studentCount = studentCount;
        }
        public long getClassId() { return classId; }
        public int getSchoolYear() { return schoolYear; }
        public String getClassName() { return className; }
        public String getTeacherName() { return teacherName; }
        public long getStudentCount() { return studentCount; }
    }
}
