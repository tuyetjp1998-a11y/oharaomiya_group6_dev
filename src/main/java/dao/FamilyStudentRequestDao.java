package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;

import bean.FamilyStudentRequest;

/**
 * 保護者と生徒の関連付け申請を管理するDAO。
 */
public class FamilyStudentRequestDao extends DAO {

    /**
     * 承認待ち申請を登録する。
     *
     * @return 新規登録できた場合はtrue
     */
    public boolean create(
            long familyUserId,
            String studentLoginId,
            int schoolYear)
            throws NamingException, SQLException {

        String sql = """
                INSERT INTO public.family_student_request (
                    family_user_id,
                    student_user_id
                )
                SELECT family.id, student.id
                FROM public.users AS family
                CROSS JOIN public.users AS student
                WHERE family.id = ?
                  AND family.role = 'FAMILY'
                  AND family.active = true
                  AND student.login_id = ?
                  AND student.role = 'STUDENT'
                  AND student.active = true
                  AND EXISTS (
                      SELECT 1
                      FROM public.student_class AS sc
                      JOIN public.teacher_class AS tc
                        ON tc.class_id = sc.class_id
                      JOIN public.users AS teacher
                        ON teacher.id = tc.teacher_user_id
                      WHERE sc.student_user_id = student.id
                        AND sc.school_year = ?
                        AND teacher.role = 'TEACHER'
                        AND teacher.active = true
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM public.family_student AS fs
                      WHERE fs.family_user_id = family.id
                        AND fs.student_user_id = student.id
                  )
                ON CONFLICT (family_user_id, student_user_id)
                    WHERE status = 'PENDING'
                DO NOTHING
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, familyUserId);
            statement.setString(2, studentLoginId);
            statement.setInt(3, schoolYear);

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * ログイン中の保護者の承認待ち申請を取得する。
     */
    public List<FamilyStudentRequest> findPendingByFamilyUserId(
            long familyUserId)
            throws NamingException, SQLException {

        List<FamilyStudentRequest> requests = new ArrayList<>();

        String sql = """
                SELECT request.id,
                       student.login_id AS student_login_id
                FROM public.family_student_request AS request
                JOIN public.users AS student
                  ON student.id = request.student_user_id
                WHERE request.family_user_id = ?
                  AND request.status = 'PENDING'
                ORDER BY request.requested_at DESC,
                         request.id DESC
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, familyUserId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    FamilyStudentRequest request =
                            new FamilyStudentRequest();

                    request.setId(resultSet.getLong("id"));
                    request.setStudentLoginId(
                            resultSet.getString("student_login_id"));

                    requests.add(request);
                }
            }
        }

        return requests;
    }
    
    /**
     * ログイン中の保護者の審査済み申請を最新20件取得する。
     */
    public List<FamilyStudentRequest> findReviewedByFamilyUserId(
            long familyUserId)
            throws NamingException, SQLException {

        List<FamilyStudentRequest> requests = new ArrayList<>();

        String sql = """
                SELECT request.id,
                       student.login_id AS student_login_id,
                       request.status,
                       request.requested_at,
                       request.reviewed_at
                FROM public.family_student_request AS request
                JOIN public.users AS student
                  ON student.id = request.student_user_id
                WHERE request.family_user_id = ?
                  AND request.status IN ('APPROVED', 'REJECTED')
                ORDER BY request.reviewed_at DESC,
                         request.id DESC
                LIMIT 20
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, familyUserId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    FamilyStudentRequest request =
                            new FamilyStudentRequest();

                    request.setId(resultSet.getLong("id"));
                    request.setStudentLoginId(
                            resultSet.getString("student_login_id"));
                    request.setStatus(
                            resultSet.getString("status"));
                    request.setRequestedAt(
                            resultSet.getObject(
                                    "requested_at",
                                    OffsetDateTime.class));
                    request.setReviewedAt(
                            resultSet.getObject(
                                    "reviewed_at",
                                    OffsetDateTime.class));

                    requests.add(request);
                }
            }
        }

        return requests;
    }
    
    /**
     * 保護者の申請履歴を最新20件取得する。
     */
    public List<FamilyStudentRequest> findHistoryByFamilyUserId(
            long familyUserId)
            throws NamingException, SQLException {

        List<FamilyStudentRequest> requests = new ArrayList<>();

        String sql = """
                SELECT request.id,
                       student.login_id AS student_login_id,
                       request.status,
                       request.requested_at,
                       request.reviewed_at
                FROM public.family_student_request AS request
                JOIN public.users AS student
                  ON student.id = request.student_user_id
                WHERE request.family_user_id = ?
                ORDER BY request.requested_at DESC,
                         request.id DESC
                
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, familyUserId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    FamilyStudentRequest request =
                            new FamilyStudentRequest();

                    request.setId(resultSet.getLong("id"));
                    request.setStudentLoginId(
                            resultSet.getString("student_login_id"));
                    request.setStatus(
                            resultSet.getString("status"));
                    request.setRequestedAt(
                            resultSet.getObject(
                                    "requested_at",
                                    OffsetDateTime.class));
                    request.setReviewedAt(
                            resultSet.getObject(
                                    "reviewed_at",
                                    OffsetDateTime.class));

                    requests.add(request);
                }
            }
        }

        return requests;
    }
    
    /**
     * 生徒ごとの最新申請が却下されている場合に取得する。
     * 関連付け済みの生徒は除外する。
     */
    public List<FamilyStudentRequest> findLatestRejected(
            long familyUserId)
            throws NamingException, SQLException {

        List<FamilyStudentRequest> requests = new ArrayList<>();

        String sql = """
                SELECT request.id,
                       student.login_id AS student_login_id
                FROM public.family_student_request AS request
                JOIN public.users AS student
                  ON student.id = request.student_user_id
                WHERE request.family_user_id = ?
                  AND request.status = 'REJECTED'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM public.family_student_request AS newer
                      WHERE newer.family_user_id = request.family_user_id
                        AND newer.student_user_id = request.student_user_id
                        AND newer.id > request.id
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM public.family_student AS fs
                      WHERE fs.family_user_id = request.family_user_id
                        AND fs.student_user_id = request.student_user_id
                  )
                ORDER BY request.reviewed_at DESC, request.id DESC
                LIMIT 20
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, familyUserId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    FamilyStudentRequest request =
                            new FamilyStudentRequest();

                    request.setId(resultSet.getLong("id"));
                    request.setStudentLoginId(
                            resultSet.getString("student_login_id"));

                    requests.add(request);
                }
            }
        }

        return requests;
    }
}