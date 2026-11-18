package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

import javax.naming.NamingException;

import bean.User;

/**
 * 生徒・保護者のログイン維持用トークンを管理する。
 */
public class LoginTokenDao extends DAO {

	/**
	 * 有効な生徒・保護者アカウントのトークンを保存する。
	 * tokenHashにはSHA-256のハッシュ値を指定する。
	 */
	public boolean save(
			long userId,
			String tokenHash,
			OffsetDateTime expiresAt)
			throws NamingException, SQLException {

		String sql = """
				INSERT INTO public.login_token
				    (user_id, token_hash, expires_at)
				SELECT id, ?, ?
				FROM public.users
				WHERE id = ?
				  AND role IN ('STUDENT', 'FAMILY')
				  AND active = TRUE
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, tokenHash);
			statement.setObject(2, expiresAt);
			statement.setLong(3, userId);

			return statement.executeUpdate() == 1;
		}
	}

	/**
	 * 有効なトークンを更新し、利用者情報を取得する。
	 * 更新前のトークンは再利用できない。
	 * 有効期限は延長しない。
	 */
	public LoginTokenData rotateAndFindUser(
			String currentTokenHash,
			String newTokenHash)
			throws NamingException, SQLException {

		String sql = """
				                WITH rotated_token AS (
				                    UPDATE public.login_token t
				                    SET token_hash = ?
				                    FROM public.users u
				                    WHERE t.token_hash = ?
				                      AND t.expires_at > CURRENT_TIMESTAMP
				                      AND u.id = t.user_id
				                      AND u.role IN ('STUDENT', 'FAMILY')
				                      AND u.active = TRUE
				        		 RETURNING t.id, t.user_id, t.expires_at
				)
				SELECT t.id AS login_token_id,
				       u.id,
				       u.login_id,
				       u.name,
				       u.role,
				       u.active,
				       u.created_at,
				       u.location_enabled,
				       u.notification_enabled,
				       t.expires_at
				                FROM rotated_token t
				                INNER JOIN public.users u
				                        ON u.id = t.user_id
				                """;

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, newTokenHash);
			statement.setString(2, currentTokenHash);

			try (ResultSet resultSet = statement.executeQuery()) {

				if (!resultSet.next()) {
					return null;
				}

				User user = new User();

				user.setId(resultSet.getLong("id"));
				user.setLoginId(
						resultSet.getString("login_id"));
				user.setName(resultSet.getString("name"));
				user.setRole(resultSet.getString("role"));
				user.setActive(
						resultSet.getBoolean("active"));
				user.setCreatedAt(
						resultSet.getObject(
								"created_at",
								OffsetDateTime.class));
				user.setLocationEnabled(
						resultSet.getBoolean(
								"location_enabled"));
				user.setNotificationEnabled(
						resultSet.getBoolean(
								"notification_enabled"));

				OffsetDateTime expiresAt = resultSet.getObject(
						"expires_at",
						OffsetDateTime.class);

				return new LoginTokenData(
				        resultSet.getLong("login_token_id"),
				        user,
				        expiresAt);
			}
		}
	}

	/**
	 * 指定された端末のトークンを削除する。
	 */
	public boolean deleteByTokenHash(String tokenHash)
			throws NamingException, SQLException {

		String sql = """
				DELETE FROM public.login_token
				WHERE token_hash = ?
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, tokenHash);

			return statement.executeUpdate() > 0;
		}
	}

	/**
	 * 自動ログイン時に使用する情報。
	 */
	public static class LoginTokenData {

	    private final long tokenId;
	    private final User user;
	    private final OffsetDateTime expiresAt;

	    public LoginTokenData(
	            long tokenId,
	            User user,
	            OffsetDateTime expiresAt) {

	        this.tokenId = tokenId;
	        this.user = user;
	        this.expiresAt = expiresAt;
	    }

	    public long getTokenId() {
	        return tokenId;
	    }

	    public User getUser() {
	        return user;
	    }

	    public OffsetDateTime getExpiresAt() {
	        return expiresAt;
	    }
	}

	/**
	 * トークンを保存し、登録されたIDを返す。
	 * 保存できなかった場合はnullを返す。
	 */
	public Long saveAndReturnId(
			long userId,
			String tokenHash,
			OffsetDateTime expiresAt)
			throws NamingException, SQLException {

		String sql = """
				INSERT INTO public.login_token
				    (user_id, token_hash, expires_at)
				SELECT id, ?, ?
				FROM public.users
				WHERE id = ?
				  AND role IN ('STUDENT', 'FAMILY')
				  AND active = TRUE
				RETURNING id
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, tokenHash);
			statement.setObject(2, expiresAt);
			statement.setLong(3, userId);

			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next()
						? resultSet.getLong("id")
						: null;
			}
		}
	}
}