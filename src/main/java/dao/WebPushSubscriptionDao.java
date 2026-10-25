package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, userId);
            statement.setString(2, endpoint);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}