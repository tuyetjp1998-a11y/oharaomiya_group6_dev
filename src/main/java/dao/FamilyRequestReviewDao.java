package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;

import bean.FamilyRequestReview;

/**
 * 担当クラスの親子関係申請を取得・審査するDAO。
 */
public class FamilyRequestReviewDao extends DAO {

    /**
     * 担当クラスの承認待ち申請を取得する。
     */
    public List<FamilyRequestReview> findPending(
            long teacherUserId, int schoolYear)
            throws NamingException, SQLException {

        List<FamilyRequestReview> requests = new ArrayList<>();

        String sql = """
                SELECT request.id,
                       family.name AS family_name,
                       family.login_id AS family_login_id,
                       student.name AS student_name,
                       student.login_id AS student_login_id,
                       classroom.class_name,
                       request.requested_at
                FROM public.family_student_request AS request
                JOIN public.users AS family
                  ON family.id = request.family_user_id
                JOIN public.users AS student
                  ON student.id = request.student_user_id
                JOIN public.student_class AS sc
                  ON sc.student_user_id = student.id
                JOIN public.school_class AS classroom
                  ON classroom.id = sc.class_id
                JOIN public.teacher_class AS tc
                  ON tc.class_id = classroom.id
                JOIN public.users AS teacher
                  ON teacher.id = tc.teacher_user_id
                WHERE request.status = 'PENDING'
                  AND tc.teacher_user_id = ?
                  AND sc.school_year = ?
                  AND teacher.role = 'TEACHER'
                  AND teacher.active = true
                  AND family.role = 'FAMILY'
                  AND family.active = true
                  AND student.role = 'STUDENT'
                  AND student.active = true
                ORDER BY request.requested_at, request.id
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, teacherUserId);
            statement.setInt(2, schoolYear);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    FamilyRequestReview request =
                            new FamilyRequestReview();

                    request.setId(resultSet.getLong("id"));
                    request.setFamilyName(
                            resultSet.getString("family_name"));
                    request.setFamilyLoginId(
                            resultSet.getString("family_login_id"));
                    request.setStudentName(
                            resultSet.getString("student_name"));
                    request.setStudentLoginId(
                            resultSet.getString("student_login_id"));
                    request.setClassName(
                            resultSet.getString("class_name"));
                    request.setRequestedAt(
                            resultSet.getObject(
                                    "requested_at",
                                    OffsetDateTime.class));

                    requests.add(request);
                }
            }
        }

        return requests;
    }

    /**
     * 担当クラスの申請を承認または却下する。
     * 承認時の関連付け登録と申請更新は同じトランザクションで行う。
     *
     * @return 審査できた場合はtrue。
     *         対象外または処理済みの場合はfalse。
     */
    public boolean review(
            long requestId,
            long teacherUserId,
            int schoolYear,
            String decision)
            throws NamingException, SQLException {

        if (!"APPROVED".equals(decision)
                && !"REJECTED".equals(decision)) {
            throw new IllegalArgumentException(
                    "不正な審査結果です。");
        }

        String selectSql = """
                SELECT request.family_user_id,
                       request.student_user_id
                FROM public.family_student_request AS request
                JOIN public.users AS family
                  ON family.id = request.family_user_id
                JOIN public.users AS student
                  ON student.id = request.student_user_id
                JOIN public.student_class AS sc
                  ON sc.student_user_id = student.id
                JOIN public.teacher_class AS tc
                  ON tc.class_id = sc.class_id
                JOIN public.users AS teacher
                  ON teacher.id = tc.teacher_user_id
                WHERE request.id = ?
                  AND request.status = 'PENDING'
                  AND tc.teacher_user_id = ?
                  AND sc.school_year = ?
                  AND teacher.role = 'TEACHER'
                  AND teacher.active = true
                  AND family.role = 'FAMILY'
                  AND family.active = true
                  AND student.role = 'STUDENT'
                  AND student.active = true
                FOR UPDATE OF request
                """;

        String insertSql = """
                INSERT INTO public.family_student (
                    family_user_id,
                    student_user_id
                )
                VALUES (?, ?)
                ON CONFLICT (family_user_id, student_user_id)
                DO NOTHING
                """;

        String updateSql = """
                UPDATE public.family_student_request
                SET status = ?,
                    reviewed_by = ?,
                    reviewed_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'PENDING'
                """;

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                long familyUserId;
                long studentUserId;

                // 担当権限を確認し、申請をロック
                try (PreparedStatement statement =
                             connection.prepareStatement(selectSql)) {

                    statement.setLong(1, requestId);
                    statement.setLong(2, teacherUserId);
                    statement.setInt(3, schoolYear);

                    try (ResultSet resultSet =
                                 statement.executeQuery()) {

                        if (!resultSet.next()) {
                            connection.rollback();
                            return false;
                        }

                        familyUserId =
                                resultSet.getLong("family_user_id");
                        studentUserId =
                                resultSet.getLong("student_user_id");
                    }
                }

                // 承認した場合だけ親子関係を登録
                if ("APPROVED".equals(decision)) {
                    try (PreparedStatement statement =
                                 connection.prepareStatement(insertSql)) {

                        statement.setLong(1, familyUserId);
                        statement.setLong(2, studentUserId);
                        statement.executeUpdate();
                    }
                }

                // 申請の審査結果を保存
                try (PreparedStatement statement =
                             connection.prepareStatement(updateSql)) {

                    statement.setString(1, decision);
                    statement.setLong(2, teacherUserId);
                    statement.setLong(3, requestId);

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException(
                                "申請の更新件数が不正です。");
                    }
                }

                connection.commit();
                return true;

            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }

                throw e;
            }
        }
    }
}