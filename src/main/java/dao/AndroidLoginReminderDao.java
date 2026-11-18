package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;

/**
 * Androidアプリのログイン期限通知対象を管理する。
 */
public class AndroidLoginReminderDao extends DAO {

    /**
     * 日本時間で期限日の7日前から、有効期限までの登録を取得する。
     */
    public List<ReminderTarget> findTargets(LocalDate date)
            throws NamingException, SQLException {

        String sql = """
                SELECT s.id,
                       s.api_login_token_id,
                       s.fcm_token
                FROM public.android_push_subscription s
                JOIN public.api_login_token t
                  ON t.id = s.api_login_token_id
                JOIN public.users u
                  ON u.id = t.user_id
                WHERE t.expires_at > CURRENT_TIMESTAMP
                  AND (t.expires_at AT TIME ZONE 'Asia/Tokyo')::date
                      <= CAST(? AS date) + 7
                  AND u.active = TRUE
                  AND u.role IN ('STUDENT', 'FAMILY')
                  AND (
                      s.last_login_reminder_on IS NULL
                      OR s.last_login_reminder_on < ?
                  )
                ORDER BY s.id
                """;

        List<ReminderTarget> targets = new ArrayList<>();

        try (Connection connection = getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setObject(1, date);
            statement.setObject(2, date);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    targets.add(new ReminderTarget(
                            resultSet.getLong("id"),
                            resultSet.getLong("api_login_token_id"),
                            resultSet.getString("fcm_token")));
                }
            }
        }

        return targets;
    }

    /**
     * 当日の送信権を取得する。
     * 複数の処理が同時実行されても、同じ登録では1日1回まで。
     * 送信前に記録するため、送信失敗時も当日は再試行しない。
     */
    public boolean claim(
            ReminderTarget target,
            LocalDate date)
            throws NamingException, SQLException {

        String sql = """
                UPDATE public.android_push_subscription s
                SET last_login_reminder_on = ?
                FROM public.api_login_token t,
                     public.users u
                WHERE s.id = ?
                  AND s.api_login_token_id = ?
                  AND s.fcm_token = ?
                  AND t.id = s.api_login_token_id
                  AND u.id = t.user_id
                  AND t.expires_at > CURRENT_TIMESTAMP
                  AND (t.expires_at AT TIME ZONE 'Asia/Tokyo')::date
                      <= CAST(? AS date) + 7
                  AND CAST(? AS date) =
                      (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Tokyo')::date
                  AND u.active = TRUE
                  AND u.role IN ('STUDENT', 'FAMILY')
                  AND (
                      s.last_login_reminder_on IS NULL
                      OR s.last_login_reminder_on < ?
                  )
                """;

        try (Connection connection = getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setObject(1, date);
            statement.setLong(2, target.getId());
            statement.setLong(3, target.getLoginTokenId());
            statement.setString(4, target.getFcmToken());
            statement.setObject(5, date);
            statement.setObject(6, date);
            statement.setObject(7, date);

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * FCMがUNREGISTEREDを返した登録だけ削除する。
     * 別のログインへ再登録された場合は削除しない。
     */
    public boolean deleteUnregistered(ReminderTarget target)
            throws NamingException, SQLException {

        String sql = """
                DELETE FROM public.android_push_subscription
                WHERE id = ?
                  AND api_login_token_id = ?
                  AND fcm_token = ?
                """;

        try (Connection connection = getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setLong(1, target.getId());
            statement.setLong(2, target.getLoginTokenId());
            statement.setString(3, target.getFcmToken());

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * 通知対象。FCMトークンはログに出力しない。
     */
    public static final class ReminderTarget {

        private final long id;
        private final long loginTokenId;
        private final String fcmToken;

        public ReminderTarget(
                long id,
                long loginTokenId,
                String fcmToken) {

            this.id = id;
            this.loginTokenId = loginTokenId;
            this.fcmToken = fcmToken;
        }

        public long getId() {
            return id;
        }

        public long getLoginTokenId() {
            return loginTokenId;
        }

        public String getFcmToken() {
            return fcmToken;
        }
    }
}