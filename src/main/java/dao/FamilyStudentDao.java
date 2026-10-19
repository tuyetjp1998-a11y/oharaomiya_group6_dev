package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;

import bean.FamilyStudent;

/**
 * 保護者に関連付けられた生徒情報を取得するDAO。
 */
public class FamilyStudentDao extends DAO {

	/**
	 * 保護者に関連付けられた生徒と最新の安否状況を取得する。
	 */
	public List<FamilyStudent> findByFamilyUserId(long familyUserId)
			throws NamingException, SQLException {

		List<FamilyStudent> students = new ArrayList<>();

		String sql = """
				         	SELECT student.id AS student_user_id,
				student.login_id AS student_login_id,
				student.name AS student_name,
				latest.status
				         FROM public.family_student AS fs
				         JOIN public.users AS family
				           ON family.id = fs.family_user_id
				         JOIN public.users AS student
				           ON student.id = fs.student_user_id
				         LEFT JOIN LATERAL (
				             SELECT report.status
				             FROM public.safety_report AS report
				             WHERE report.user_id = student.id
				             ORDER BY report.reported_at DESC,
				                      report.id DESC
				             LIMIT 1
				         ) AS latest ON true
				         WHERE fs.family_user_id = ?
				           AND family.role = 'FAMILY'
				           AND family.active = true
				           AND student.role = 'STUDENT'
				           AND student.active = true
				         ORDER BY student.name, student.id
				         """;

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, familyUserId);

			try (ResultSet resultSet = statement.executeQuery()) {

				while (resultSet.next()) {
					FamilyStudent student = new FamilyStudent();

					student.setStudentUserId(
							resultSet.getLong("student_user_id"));
					student.setStudentLoginId(
							resultSet.getString("student_login_id"));
					student.setStudentName(
							resultSet.getString("student_name"));
					student.setStatus(
							resultSet.getString("status"));

					students.add(student);
				}
			}
		}

		return students;
	}

	/**
	 * 保護者に関連付けられた生徒を1件取得する。
	 * 関連付けがない場合はnullを返す。
	 */
	public FamilyStudent findLinkedStudent(
			long familyUserId, long studentUserId)
			throws NamingException, SQLException {

		String sql = """
				SELECT student.id AS student_user_id,
				       student.name AS student_name
				FROM public.family_student AS fs
				JOIN public.users AS family
				  ON family.id = fs.family_user_id
				JOIN public.users AS student
				  ON student.id = fs.student_user_id
				WHERE fs.family_user_id = ?
				  AND fs.student_user_id = ?
				  AND family.role = 'FAMILY'
				  AND family.active = true
				  AND student.role = 'STUDENT'
				  AND student.active = true
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, familyUserId);
			statement.setLong(2, studentUserId);

			try (ResultSet resultSet = statement.executeQuery()) {

				if (!resultSet.next()) {
					return null;
				}

				FamilyStudent student = new FamilyStudent();

				student.setStudentUserId(
						resultSet.getLong("student_user_id"));
				student.setStudentName(
						resultSet.getString("student_name"));

				return student;
			}
		}
	}
}