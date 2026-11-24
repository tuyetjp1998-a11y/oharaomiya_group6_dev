package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;

import bean.TeacherClassSummary;

public class TeacherClassDao extends DAO {

    /** ログイン中の教員が担当するクラスと有効な生徒の人数を年度別に取得する。 */
    public List<TeacherClassSummary> findAssignedClasses(long teacherId, int schoolYear)
            throws NamingException, SQLException {
        String sql = """
                SELECT c.id AS class_id, c.class_name, s.id AS student_id,
                       s.login_id AS student_login_id, s.name AS student_name, latest.status, latest.danger_type,
                       latest.report_id, latest.reported_at
                FROM public.teacher_class AS tc
                JOIN public.users AS t ON t.id = tc.teacher_user_id
                JOIN public.school_class AS c ON c.id = tc.class_id
                LEFT JOIN public.student_class AS sc
                  ON sc.class_id = c.id AND sc.school_year = c.school_year
                LEFT JOIN public.users AS s
                  ON s.id = sc.student_user_id AND s.role = 'STUDENT' AND s.active
                LEFT JOIN LATERAL (
                    SELECT r.id AS report_id, r.status, r.danger_type, r.reported_at FROM public.safety_report AS r
                    WHERE r.user_id = s.id
                    ORDER BY r.reported_at DESC, r.id DESC
                    LIMIT 1
                ) AS latest ON TRUE
                WHERE t.id = ? AND t.role = 'TEACHER' AND t.active
                  AND c.school_year = ?
                ORDER BY c.class_name, c.id, latest.reported_at DESC NULLS LAST, s.id
                """;
        List<TeacherClassSummary> classes = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherId);
            statement.setInt(2, schoolYear);
            try (ResultSet rows = statement.executeQuery()) {
                TeacherClassSummary room = null;
                long previousClassId = 0;
                while (rows.next()) {
                    long classId = rows.getLong("class_id");
                    if (room == null || classId != previousClassId) {
                        room = new TeacherClassSummary(classId, rows.getString("class_name"));
                        classes.add(room);
                        previousClassId = classId;
                    }
                    if (rows.getObject("student_id") != null) {
                        room.addStudent(rows.getLong("student_id"), rows.getString("student_login_id"),
                                rows.getString("student_name"),
                                rows.getString("status"), rows.getString("danger_type"),
                                rows.getLong("report_id"), rows.getObject("reported_at", OffsetDateTime.class));
                    }
                }
            }
        }
        return classes;
    }

    /** 担当権限を確認して今年度の所属を登録する。既存の所属は上書きしない。 */
    public String addRegisteredStudent(long teacherId, int year, long classId, String loginId)
            throws NamingException, SQLException {
        String sql = """
                WITH assigned AS (
                    SELECT c.id, c.school_year FROM public.school_class c
                    JOIN public.teacher_class tc ON tc.class_id = c.id
                    JOIN public.users t ON t.id = tc.teacher_user_id
                    WHERE t.id = ? AND t.role = 'TEACHER' AND t.active
                      AND c.school_year = ? AND c.id = ?
                ), student AS (
                    SELECT id FROM public.users
                    WHERE login_id = ? AND role = 'STUDENT' AND active
                ), added AS (
                    INSERT INTO public.student_class (student_user_id, school_year, class_id)
                    SELECT s.id, a.school_year, a.id FROM student s CROSS JOIN assigned a
                    ON CONFLICT (student_user_id, school_year) DO NOTHING
                    RETURNING class_id
                )
                SELECT CASE
                    WHEN NOT EXISTS (SELECT 1 FROM assigned) THEN 'FORBIDDEN'
                    WHEN NOT EXISTS (SELECT 1 FROM student) THEN 'UNKNOWN'
                    WHEN EXISTS (SELECT 1 FROM added) THEN 'ADDED'
                    WHEN EXISTS (SELECT 1 FROM public.student_class sc
                        JOIN student s ON s.id = sc.student_user_id
                        JOIN assigned a ON a.id = sc.class_id AND a.school_year = sc.school_year)
                        THEN 'ALREADY'
                    ELSE 'ASSIGNED'
                END
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherId);
            statement.setInt(2, year);
            statement.setLong(3, classId);
            statement.setString(4, loginId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getString(1);
            }
        }
    }

    /** 教員が生徒アカウントを作成し、担当クラスへ登録する。 */
    public String registerStudentByProxy(
            long teacherId,
            int year,
            long classId,
            String loginId,
            String name,
            String passwordHash)
            throws NamingException, SQLException {
        String sql = """
                WITH assigned AS (
                    SELECT c.id, c.school_year
                    FROM public.school_class c
                    JOIN public.teacher_class tc ON tc.class_id = c.id
                    JOIN public.users teacher ON teacher.id = tc.teacher_user_id
                    WHERE teacher.id = ?
                      AND teacher.role = 'TEACHER'
                      AND teacher.active
                      AND c.school_year = ?
                      AND c.id = ?
                ), new_student AS (
                    INSERT INTO public.users
                        (login_id, name, password_hash, role, active, location_enabled)
                    SELECT ?, ?, ?, 'STUDENT', TRUE, FALSE
                    FROM assigned
                    ON CONFLICT (login_id) DO NOTHING
                    RETURNING id
                ), added AS (
                    INSERT INTO public.student_class
                        (student_user_id, school_year, class_id)
                    SELECT student.id, assigned.school_year, assigned.id
                    FROM new_student student
                    CROSS JOIN assigned
                    RETURNING student_user_id
                )
                SELECT CASE
                    WHEN NOT EXISTS (SELECT 1 FROM assigned) THEN 'FORBIDDEN'
                    WHEN EXISTS (SELECT 1 FROM added) THEN 'ADDED'
                    WHEN EXISTS (
                        SELECT 1 FROM public.users WHERE login_id = ?
                    ) THEN 'DUPLICATE'
                    ELSE 'FAILED'
                END
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherId);
            statement.setInt(2, year);
            statement.setLong(3, classId);
            statement.setString(4, loginId);
            statement.setString(5, name);
            statement.setString(6, passwordHash);
            statement.setString(7, loginId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getString(1);
            }
        }
    }

    /** 今年度の担当クラスに一致する所属だけを解除する。 */
    public boolean removeStudent(long teacherId, int year, long classId, long studentId)
            throws NamingException, SQLException {
        String sql = """
                DELETE FROM public.student_class
                WHERE student_user_id = ? AND school_year = ? AND class_id = ?
                  AND EXISTS (
                      SELECT 1 FROM public.teacher_class tc
                      JOIN public.users t ON t.id = tc.teacher_user_id
                      WHERE tc.class_id = student_class.class_id AND t.id = ?
                        AND t.role = 'TEACHER' AND t.active
                  )
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentId);
            statement.setInt(2, year);
            statement.setLong(3, classId);
            statement.setLong(4, teacherId);
            return statement.executeUpdate() == 1;
        }
    }
}
