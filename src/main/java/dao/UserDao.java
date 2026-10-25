package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

import javax.naming.NamingException;

import bean.User;

/**
 * 利用者情報を取得するDAO。
 */
public class UserDao extends DAO {

	/**
	 * ログインIDで利用者情報とパスワードハッシュを取得する。
	 *
	 * @return 該当する利用者が存在しない場合はnull
	 */

	public LoginData findByLoginId(String loginId)
			throws NamingException, SQLException {

		String sql = """
				      SELECT id,
				             login_id,
				             name,
				             password_hash,
				             role,
				             active,
				             created_at,
				             location_enabled,
				notification_enabled
				      FROM public.users
				      WHERE login_id = ?
				      """;

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			// 検索条件をセット
			statement.setString(1, loginId);

			try (ResultSet resultSet = statement.executeQuery()) {

				// 利用者が存在しない場合
				if (!resultSet.next()) {
					return null;
				}

				// DBの値をUserにセット
				User user = new User();

				user.setId(resultSet.getLong("id"));
				user.setLoginId(resultSet.getString("login_id"));
				user.setName(resultSet.getString("name"));
				user.setRole(resultSet.getString("role"));
				user.setActive(resultSet.getBoolean("active"));
				user.setCreatedAt(
						resultSet.getObject(
								"created_at",
								OffsetDateTime.class));
				user.setLocationEnabled(
						resultSet.getBoolean("location_enabled"));

				user.setNotificationEnabled(
						resultSet.getBoolean("notification_enabled"));

				// パスワードハッシュはUserとは別に保持する
				String passwordHash = resultSet.getString("password_hash");

				return new LoginData(user, passwordHash);
			}
		}
	}

	/**
	 * 最新の位置情報利用設定を取得する。
	 */
	public boolean getLocationEnabled(long userId)
			throws NamingException, SQLException {

		String sql = """
				SELECT location_enabled
				FROM public.users
				WHERE id = ?
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, userId);

			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					throw new SQLException(
							"利用者が存在しません。");
				}

				return resultSet.getBoolean("location_enabled");
			}
		}
	}

	/**
	 * 有効な生徒アカウントの位置情報利用設定を更新する。
	 */
	public boolean updateLocationEnabled(
			long userId,
			boolean enabled)
			throws NamingException, SQLException {

		String sql = """
				UPDATE public.users
				SET location_enabled = ?
				WHERE id = ?
				  AND active = TRUE
				  AND role = 'STUDENT'
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setBoolean(1, enabled);
			statement.setLong(2, userId);

			return statement.executeUpdate() == 1;
		}
	}

	/**
	 * ログイン処理で使用するデータ。
	 * このオブジェクトはセッションに保存しない。
	 */
	/**
	 * 保護者アカウントと親子関係の追加申請を同時に登録する。
	 *
	 * @return 登録成功の場合はtrue。
	 *         ID重複、または申請対象の生徒が見つからない場合はfalse。
	 */
	public boolean registerFamily(
			String loginId,
			String name,
			String passwordHash,
			String studentLoginId,
			int schoolYear)
			throws NamingException, SQLException {

		String sql = """
				WITH target_student AS (
				    SELECT student.id
				    FROM public.users student
				    WHERE student.login_id = ?
				      AND student.role = 'STUDENT'
				      AND student.active = TRUE
				      AND EXISTS (
				          SELECT 1
				          FROM public.student_class sc
				          JOIN public.teacher_class tc
				            ON tc.class_id = sc.class_id
				          JOIN public.users teacher
				            ON teacher.id = tc.teacher_user_id
				          WHERE sc.student_user_id = student.id
				            AND sc.school_year = ?
				            AND teacher.role = 'TEACHER'
				            AND teacher.active = TRUE
				      )
				),
				new_family AS (
				    INSERT INTO public.users
				        (login_id, name, password_hash,
				         role, active, location_enabled)
				    SELECT ?, ?, ?, 'FAMILY', TRUE, FALSE
				    WHERE EXISTS (
				        SELECT 1 FROM target_student
				    )
				    ON CONFLICT (login_id) DO NOTHING
				    RETURNING id
				)
				INSERT INTO public.family_student_request
				    (family_user_id, student_user_id, status)
				SELECT family.id, student.id, 'PENDING'
				FROM new_family family
				CROSS JOIN target_student student
				RETURNING id
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			// 生徒の検索条件
			statement.setString(1, studentLoginId);
			statement.setInt(2, schoolYear);

			// 保護者アカウントの登録内容
			statement.setString(3, loginId);
			statement.setString(4, name);
			statement.setString(5, passwordHash);

			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next();
			}
		}
	}

	/**
	 * 有効な保護者アカウントの通知設定を取得する。
	 */
	public boolean getNotificationEnabled(long userId)
			throws NamingException, SQLException {

		String sql = """
				SELECT notification_enabled
				FROM public.users
				WHERE id = ?
				  AND active = TRUE
				  AND role = 'FAMILY'
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, userId);

			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					throw new SQLException(
							"有効な保護者アカウントが存在しません。");
				}

				return resultSet.getBoolean(
						"notification_enabled");
			}
		}
	}

	/**
	 * 有効な保護者アカウントの通知設定を更新する。
	 *
	 * @return 更新成功の場合はtrue
	 */
	public boolean updateNotificationEnabled(
			long userId,
			boolean enabled)
			throws NamingException, SQLException {

		String sql = """
				UPDATE public.users
				SET notification_enabled = ?
				WHERE id = ?
				  AND active = TRUE
				  AND role = 'FAMILY'
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setBoolean(1, enabled);
			statement.setLong(2, userId);

			return statement.executeUpdate() == 1;
		}
	}

	public static class LoginData {

		private final User user;
		private final String passwordHash;

		public LoginData(User user, String passwordHash) {
			this.user = user;
			this.passwordHash = passwordHash;
		}

		public User getUser() {
			return user;
		}

		public String getPasswordHash() {
			return passwordHash;
		}
	}
}