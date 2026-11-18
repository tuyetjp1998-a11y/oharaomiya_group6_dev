package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;

import bean.WebPushSubscription;

/**
 * 保護者のWeb Push登録情報を管理する。
 */
public class WebPushSubscriptionDao extends DAO {

	/**
	 * ブラウザーの登録情報を保存し、通知を有効にする。
	 * 別の利用者が所有する登録情報は上書きしない。
	 */
	public boolean enable(
			long userId,
			String endpoint,
			String p256dh,
			String auth)
			throws NamingException, SQLException {

		String sql = """
						WITH target_family AS (
						    SELECT id
						    FROM public.users
						    WHERE id = ?
						      AND role = 'FAMILY'
						      AND active = TRUE
						    FOR UPDATE
						),
						saved_subscription AS (
						    INSERT INTO public.web_push_subscription
						        (user_id, endpoint, p256dh, auth)
						    SELECT id, ?, ?, ?
						    FROM target_family
						    ON CONFLICT (endpoint) DO UPDATE
						    SET p256dh = EXCLUDED.p256dh,
						        auth = EXCLUDED.auth,
						        updated_at = CURRENT_TIMESTAMP
						    WHERE web_push_subscription.user_id
				    = EXCLUDED.user_id
				AND web_push_subscription.expired_at IS NULL
						    RETURNING user_id
						)
						UPDATE public.users
						SET notification_enabled = TRUE
						WHERE id = ?
						  AND EXISTS (
						      SELECT 1
						      FROM saved_subscription
						      WHERE user_id = users.id
						  )
						RETURNING id
						""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, userId);
			statement.setString(2, endpoint);
			statement.setString(3, p256dh);
			statement.setString(4, auth);
			statement.setLong(5, userId);

			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next();
			}
		}
	}

	/**
	 * 保護者アカウント全体の通知を無効にする。
	 * 再度有効にできるよう、登録情報は保持する。
	 */
	public boolean disable(long userId)
			throws NamingException, SQLException {

		String sql = """
				UPDATE public.users
				SET notification_enabled = FALSE
				WHERE id = ?
				  AND role = 'FAMILY'
				  AND active = TRUE
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, userId);

			return statement.executeUpdate() == 1;
		}
	}

	/**
	 * 指定された生徒と関連付けられた保護者の通知登録を取得する。
	 * 有効な保護者アカウントで、通知がONの場合のみ取得する。
	 */
	public List<WebPushSubscription> findByStudentUserId(
			long studentUserId)
			throws NamingException, SQLException {

		List<WebPushSubscription> subscriptions = new ArrayList<>();

		String sql = """
				SELECT s.id,
				       s.user_id,
				       s.endpoint,
				       s.p256dh,
				       s.auth
				FROM public.web_push_subscription s
				INNER JOIN public.users u
				        ON u.id = s.user_id
				WHERE u.role = 'FAMILY'
				  AND u.active = TRUE
				  AND u.notification_enabled = TRUE
				  AND s.expired_at IS NULL
				  AND EXISTS (
				      SELECT 1
				      FROM public.family_student fs
				      WHERE fs.family_user_id = u.id
				        AND fs.student_user_id = ?
				  )
				ORDER BY s.id
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, studentUserId);

			try (ResultSet resultSet = statement.executeQuery()) {

				while (resultSet.next()) {
					WebPushSubscription subscription = new WebPushSubscription();

					subscription.setId(
							resultSet.getLong("id"));
					subscription.setUserId(
							resultSet.getLong("user_id"));
					subscription.setEndpoint(
							resultSet.getString("endpoint"));
					subscription.setP256dh(
							resultSet.getString("p256dh"));
					subscription.setAuth(
							resultSet.getString("auth"));

					subscriptions.add(subscription);
				}
			}
		}

		return subscriptions;
	}

	/**
	 * 有効期限切れの通知登録を削除する。
	 * 取得後に更新された登録は削除しない。
	 */
	public boolean deleteExpired(
			WebPushSubscription subscription)
			throws NamingException, SQLException {

		String sql = """
				DELETE FROM public.web_push_subscription
				WHERE id = ?
				  AND user_id = ?
				  AND endpoint = ?
				  AND p256dh = ?
				  AND auth = ?
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, subscription.getId());
			statement.setLong(2, subscription.getUserId());
			statement.setString(3, subscription.getEndpoint());
			statement.setString(4, subscription.getP256dh());
			statement.setString(5, subscription.getAuth());

			return statement.executeUpdate() == 1;
		}
	}

	/**
	 * ログアウトするブラウザーの通知登録を削除する。
	 * 他の利用者や他の端末の登録は削除しない。
	 */
	public boolean deleteByUserAndEndpoint(
			long userId,
			String endpoint)
			throws NamingException, SQLException {

		String sql = """
				DELETE FROM public.web_push_subscription
				WHERE user_id = ?
				  AND endpoint = ?
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, userId);
			statement.setString(2, endpoint);

			return statement.executeUpdate() > 0;
		}
	}

	/**
	 * この通知登録が、通知を有効にしている保護者のものか確認する。
	 */
	public boolean isEnabledForEndpoint(
			long userId,
			String endpoint)
			throws NamingException, SQLException {

		String sql = """
				SELECT 1
				FROM public.web_push_subscription s
				INNER JOIN public.users u
				        ON u.id = s.user_id
				WHERE s.user_id = ?
				  AND s.endpoint = ?
				  AND u.role = 'FAMILY'
				  AND u.active = TRUE
				  AND u.notification_enabled = TRUE
				  AND s.expired_at IS NULL
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, userId);
			statement.setString(2, endpoint);

			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next();
			}
		}
	}

	/**
	 * Pushサービスで無効と判定された登録を期限切れにする。
	 * 別の登録や鍵が変更された登録には影響させない。
	 */
	public boolean markExpired(WebPushSubscription subscription)
			throws NamingException, SQLException {

		String sql = """
				UPDATE public.web_push_subscription
				SET expired_at = CURRENT_TIMESTAMP
				WHERE id = ?
				  AND user_id = ?
				  AND endpoint = ?
				  AND p256dh = ?
				  AND auth = ?
				  AND expired_at IS NULL
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, subscription.getId());
			statement.setLong(2, subscription.getUserId());
			statement.setString(3, subscription.getEndpoint());
			statement.setString(4, subscription.getP256dh());
			statement.setString(5, subscription.getAuth());

			return statement.executeUpdate() == 1;
		}
	}

	/**
	 * 利用者が所有する登録が期限切れか確認する。
	 */
	public boolean isExpiredForEndpoint(
			long userId, String endpoint)
			throws NamingException, SQLException {

		String sql = """
				SELECT 1
				FROM public.web_push_subscription
				WHERE user_id = ?
				  AND endpoint = ?
				  AND expired_at IS NOT NULL
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, userId);
			statement.setString(2, endpoint);

			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next();
			}
		}
	}

	/**
	 * この端末の通知登録をログイン維持用トークンに関連付ける。
	 * 同じ利用者の有効なトークンのみ使用する。
	 */
	public boolean bindLoginToken(
			long userId,
			String endpoint,
			long loginTokenId)
			throws NamingException, SQLException {

		String sql = """
				UPDATE public.web_push_subscription s
				SET login_token_id = t.id,
				    last_login_reminder_on =
				        CASE
				            WHEN s.login_token_id IS DISTINCT FROM t.id
				            THEN NULL
				            ELSE s.last_login_reminder_on
				        END
				FROM public.login_token t
				INNER JOIN public.users u
				        ON u.id = t.user_id
				WHERE s.user_id = ?
				  AND s.endpoint = ?
				  AND s.expired_at IS NULL
				  AND t.id = ?
				  AND t.user_id = s.user_id
				  AND t.expires_at > CURRENT_TIMESTAMP
				  AND u.active = TRUE
				  AND u.role IN ('STUDENT', 'FAMILY')
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, userId);
			statement.setString(2, endpoint);
			statement.setLong(3, loginTokenId);

			return statement.executeUpdate() == 1;
		}
	}

	/**
	 * 生徒の通知登録を、この端末の有効なトークンと関連付けて保存する。
	 * 別の利用者の登録情報は上書きしない。
	 */
	public boolean saveStudentSubscription(
			long userId,
			long loginTokenId,
			String endpoint,
			String p256dh,
			String auth)
			throws NamingException, SQLException {

		String sql = """
				INSERT INTO public.web_push_subscription
				    (user_id, endpoint, p256dh, auth, login_token_id)
				SELECT t.user_id, ?, ?, ?, t.id
				FROM public.login_token t
				INNER JOIN public.users u
				        ON u.id = t.user_id
				WHERE t.user_id = ?
				  AND t.id = ?
				  AND t.expires_at > CURRENT_TIMESTAMP
				  AND u.role = 'STUDENT'
				  AND u.active = TRUE
				ON CONFLICT (endpoint) DO UPDATE
				SET p256dh = EXCLUDED.p256dh,
				    auth = EXCLUDED.auth,
				    login_token_id = EXCLUDED.login_token_id,
				    updated_at = CURRENT_TIMESTAMP,
				    last_login_reminder_on =
				        CASE
				            WHEN web_push_subscription.login_token_id
				                 IS DISTINCT FROM EXCLUDED.login_token_id
				            THEN NULL
				            ELSE web_push_subscription.last_login_reminder_on
				        END
				WHERE web_push_subscription.user_id = EXCLUDED.user_id
				  AND web_push_subscription.expired_at IS NULL
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, endpoint);
			statement.setString(2, p256dh);
			statement.setString(3, auth);
			statement.setLong(4, userId);
			statement.setLong(5, loginTokenId);

			return statement.executeUpdate() == 1;
		}
	}

	/**
	 * 指定された端末がログイン期限通知を受信できるか確認する。
	 * 有効期限まで7日以内の有効なトークンだけを対象にする。
	 */
	public boolean canReceiveLoginReminder(
			String tokenHash, String endpoint)
			throws NamingException, SQLException {
		return hasLoginReminderSubscription(tokenHash, endpoint, true);
	}

	public boolean isLoginReminderRegistered(
			String tokenHash, String endpoint)
			throws NamingException, SQLException {
		return hasLoginReminderSubscription(tokenHash, endpoint, false);
	}

	private boolean hasLoginReminderSubscription(
			String tokenHash, String endpoint, boolean requireReminderWindow)
			throws NamingException, SQLException {
		String sql = """
								SELECT 1
								FROM public.web_push_subscription s
								INNER JOIN public.login_token t
								        ON t.id = s.login_token_id
								       AND t.user_id = s.user_id
								INNER JOIN public.users u
								        ON u.id = t.user_id
								WHERE t.token_hash = ?
								  AND s.endpoint = ?
								  AND s.expired_at IS NULL
								  AND t.expires_at > CURRENT_TIMESTAMP
								  AND (
				    NOT ?
				    OR t.expires_at <= CURRENT_TIMESTAMP + INTERVAL '7 days'
				)
								  AND u.active = TRUE
								  AND u.role IN ('STUDENT', 'FAMILY')
								""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, tokenHash);
			statement.setString(2, endpoint);
			statement.setBoolean(3, requireReminderWindow);
			try (ResultSet resultSet = statement.executeQuery()) {

				return resultSet.next();
			}
		}
	}

	/**
	 * ログイン有効期限まで7日以内の通知登録を取得する。
	 * 指定日に通知済みの登録は対象外にする。
	 */
	public List<WebPushSubscription> findLoginReminderTargets(
			LocalDate reminderDate)
			throws NamingException, SQLException {

		List<WebPushSubscription> subscriptions = new ArrayList<>();

		String sql = """
				   SELECT s.id,
				s.user_id,
				s.endpoint,
				s.p256dh,
				s.auth,
				s.login_token_id
				   FROM public.web_push_subscription s
				   INNER JOIN public.login_token t
				           ON t.id = s.login_token_id
				          AND t.user_id = s.user_id
				   INNER JOIN public.users u
				           ON u.id = s.user_id
				   WHERE s.expired_at IS NULL
				     AND t.expires_at > CURRENT_TIMESTAMP
				     AND t.expires_at <=
				         CURRENT_TIMESTAMP + INTERVAL '7 days'
				     AND u.active = TRUE
				     AND u.role IN ('STUDENT', 'FAMILY')
				     AND (
				         s.last_login_reminder_on IS NULL
				         OR s.last_login_reminder_on < ?
				     )
				   ORDER BY s.id
				   """;

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setObject(1, reminderDate);

			try (ResultSet resultSet = statement.executeQuery()) {

				while (resultSet.next()) {
					WebPushSubscription subscription = new WebPushSubscription();

					subscription.setId(
							resultSet.getLong("id"));
					subscription.setUserId(
							resultSet.getLong("user_id"));
					subscription.setEndpoint(
							resultSet.getString("endpoint"));
					subscription.setP256dh(
							resultSet.getString("p256dh"));
					subscription.setAuth(
							resultSet.getString("auth"));
					// 取得時点のトークンIDを保持する
					subscription.setLoginTokenId(
							resultSet.getLong("login_token_id"));
					subscriptions.add(subscription);
				}
			}
		}

		return subscriptions;
	}

	/**
	 * この通知登録の当日の送信権を取得する。
	 * トークンや登録情報が変わった場合は送信対象にしない。
	 */
	public boolean claimLoginReminder(
			WebPushSubscription subscription,
			LocalDate reminderDate)
			throws NamingException, SQLException {

		if (subscription.getLoginTokenId() == null) {
			return false;
		}

		String sql = """
				UPDATE public.web_push_subscription s
				SET last_login_reminder_on = ?
				WHERE s.id = ?
				  AND s.user_id = ?
				  AND s.login_token_id = ?
				  AND s.endpoint = ?
				  AND s.p256dh = ?
				  AND s.auth = ?
				  AND s.expired_at IS NULL
				  AND (
				      s.last_login_reminder_on IS NULL
				      OR s.last_login_reminder_on < ?
				  )
				  AND EXISTS (
				      SELECT 1
				      FROM public.login_token t
				      INNER JOIN public.users u
				              ON u.id = t.user_id
				      WHERE t.id = s.login_token_id
				        AND t.user_id = s.user_id
				        AND t.expires_at > CURRENT_TIMESTAMP
				        AND t.expires_at <=
				            CURRENT_TIMESTAMP + INTERVAL '7 days'
				        AND u.active = TRUE
				        AND u.role IN ('STUDENT', 'FAMILY')
				  )
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setObject(1, reminderDate);
			statement.setLong(2, subscription.getId());
			statement.setLong(3, subscription.getUserId());
			statement.setLong(
					4, subscription.getLoginTokenId());
			statement.setString(
					5, subscription.getEndpoint());
			statement.setString(
					6, subscription.getP256dh());
			statement.setString(
					7, subscription.getAuth());
			statement.setObject(8, reminderDate);

			return statement.executeUpdate() == 1;
		}
	}

	/**
	 * 保護者のログイン期限通知用の登録情報を保存する。
	 * 安否通知のON／OFF設定は変更しない。
	 */
	public boolean saveFamilyReminderSubscription(
			long userId,
			long loginTokenId,
			String endpoint,
			String p256dh,
			String auth)
			throws NamingException, SQLException {

		String sql = """
				INSERT INTO public.web_push_subscription AS s
				    (user_id, login_token_id,
				     endpoint, p256dh, auth)
				SELECT u.id, t.id, ?, ?, ?
				FROM public.users u
				INNER JOIN public.login_token t
				        ON t.user_id = u.id
				WHERE u.id = ?
				  AND t.id = ?
				  AND u.role = 'FAMILY'
				  AND u.active = TRUE
				  AND t.expires_at > CURRENT_TIMESTAMP
				ON CONFLICT (endpoint) DO UPDATE
				SET p256dh = EXCLUDED.p256dh,
				    auth = EXCLUDED.auth,
				    login_token_id = EXCLUDED.login_token_id,
				    last_login_reminder_on =
				        CASE
				            WHEN s.login_token_id
				                 IS DISTINCT FROM
				                 EXCLUDED.login_token_id
				            THEN NULL
				            ELSE s.last_login_reminder_on
				        END,
				    updated_at = CURRENT_TIMESTAMP
				WHERE s.user_id = EXCLUDED.user_id
				  AND s.expired_at IS NULL
				RETURNING id
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, endpoint);
			statement.setString(2, p256dh);
			statement.setString(3, auth);
			statement.setLong(4, userId);
			statement.setLong(5, loginTokenId);

			try (ResultSet resultSet = statement.executeQuery()) {

				return resultSet.next();
			}
		}
	}
}