package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;

/**
 * AndroidアプリのFCM登録情報を管理する。
 */
public class AndroidPushSubscriptionDao extends DAO {

    /**
     * 有効なアクセストークンに対応するFCM登録を保存する。
     *
     * @return 認証が有効で保存できた場合はtrue
     */
    public boolean save(
            String accessTokenHash,
            String fcmToken)
            throws NamingException, SQLException {

        if (accessTokenHash == null
                || fcmToken == null
                || fcmToken.isBlank()
                || fcmToken.length() > 2048
                || !fcmToken.matches("[\\x21-\\x7E]+")) {

            return false;
        }

        String findLoginSql = """
                SELECT t.id
                FROM public.api_login_token t
                JOIN public.users u ON u.id = t.user_id
                WHERE t.access_token_hash = ?
                  AND t.access_expires_at > CURRENT_TIMESTAMP
                  AND t.expires_at > CURRENT_TIMESTAMP
                  AND u.active = TRUE
                  AND u.role IN ('STUDENT', 'FAMILY')
                FOR UPDATE OF t
                """;

        // 同じログインに古いFCMトークンがある場合は置き換える。
        String deleteOldSql = """
                DELETE FROM public.android_push_subscription
                WHERE api_login_token_id = ?
                  AND fcm_token <> ?
                """;

        // 同じFCMトークンは現在のログインへ関連付ける。
        String saveSql = """
                INSERT INTO public.android_push_subscription AS s
                    (api_login_token_id, fcm_token)
                VALUES (?, ?)
                ON CONFLICT (fcm_token)
                DO UPDATE SET
                    api_login_token_id =
                        EXCLUDED.api_login_token_id,
                    updated_at = CURRENT_TIMESTAMP,
                    last_login_reminder_on =
                        CASE
                            WHEN s.api_login_token_id =
                                 EXCLUDED.api_login_token_id
                            THEN s.last_login_reminder_on
                            ELSE NULL
                        END
                """;

        try (Connection connection = getConnection()) {
            boolean originalAutoCommit =
                    connection.getAutoCommit();

            connection.setAutoCommit(false);

            try {
                long loginTokenId;

                try (PreparedStatement statement =
                        connection.prepareStatement(findLoginSql)) {

                    statement.setString(1, accessTokenHash);

                    try (ResultSet resultSet =
                            statement.executeQuery()) {

                        if (!resultSet.next()) {
                            connection.rollback();
                            return false;
                        }

                        loginTokenId = resultSet.getLong("id");
                    }
                }

                try (PreparedStatement statement =
                        connection.prepareStatement(deleteOldSql)) {

                    statement.setLong(1, loginTokenId);
                    statement.setString(2, fcmToken);
                    statement.executeUpdate();
                }

                try (PreparedStatement statement =
                        connection.prepareStatement(saveSql)) {

                    statement.setLong(1, loginTokenId);
                    statement.setString(2, fcmToken);
                    statement.executeUpdate();
                }

                connection.commit();
                return true;

            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                throw e;

            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }
    /**
     * 関連する保護者の有効なAndroid通知先を取得する。
     * アクセストークンの15分の期限ではなく、
     * ログイン全体の有効期限を確認する。
     */
    public List<FamilyNotificationTarget> findFamilyTargets(
            long studentUserId)
            throws NamingException, SQLException {

        String sql = """
                SELECT s.id,
                       s.api_login_token_id,
                       s.fcm_token
                FROM public.android_push_subscription s
                JOIN public.api_login_token t
                  ON t.id = s.api_login_token_id
                JOIN public.users family
                  ON family.id = t.user_id
                WHERE family.role = 'FAMILY'
                  AND family.active = TRUE
                  AND family.notification_enabled = TRUE
                  AND t.expires_at > CURRENT_TIMESTAMP
                  AND EXISTS (
                      SELECT 1
                      FROM public.family_student fs
                      JOIN public.users student
                        ON student.id = fs.student_user_id
                      WHERE fs.family_user_id = family.id
                        AND fs.student_user_id = ?
                        AND student.role = 'STUDENT'
                        AND student.active = TRUE
                  )
                ORDER BY s.id
                """;

        List<FamilyNotificationTarget> targets =
                new ArrayList<>();

        try (Connection connection = getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setLong(1, studentUserId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    targets.add(
                            new FamilyNotificationTarget(
                                    resultSet.getLong("id"),
                                    resultSet.getLong(
                                            "api_login_token_id"),
                                    resultSet.getString("fcm_token")));
                }
            }
        }

        return targets;
    }

    /**
     * 送信直前に通知先・設定・関連付けを再確認する。
     */
    public boolean isFamilyTargetEnabled(
            long studentUserId,
            FamilyNotificationTarget target)
            throws NamingException, SQLException {

        String sql = """
                SELECT 1
                FROM public.android_push_subscription s
                JOIN public.api_login_token t
                  ON t.id = s.api_login_token_id
                JOIN public.users family
                  ON family.id = t.user_id
                WHERE s.id = ?
                  AND s.api_login_token_id = ?
                  AND s.fcm_token = ?
                  AND family.role = 'FAMILY'
                  AND family.active = TRUE
                  AND family.notification_enabled = TRUE
                  AND t.expires_at > CURRENT_TIMESTAMP
                  AND EXISTS (
                      SELECT 1
                      FROM public.family_student fs
                      JOIN public.users student
                        ON student.id = fs.student_user_id
                      WHERE fs.family_user_id = family.id
                        AND fs.student_user_id = ?
                        AND student.role = 'STUDENT'
                        AND student.active = TRUE
                  )
                """;

        try (Connection connection = getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setLong(1, target.getSubscriptionId());
            statement.setLong(2, target.getLoginTokenId());
            statement.setString(3, target.getFcmToken());
            statement.setLong(4, studentUserId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    /**
     * FCMがUNREGISTEREDと返した登録だけを削除する。
     * 取得後に別のログインへ変更された登録は削除しない。
     */
    public void deleteUnregisteredFamilyTarget(
            FamilyNotificationTarget target)
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

            statement.setLong(1, target.getSubscriptionId());
            statement.setLong(2, target.getLoginTokenId());
            statement.setString(3, target.getFcmToken());

            statement.executeUpdate();
        }
    }

    /** 配信対象者の有効なAndroid通知先。 */
    private static final String DISPATCH_TARGETS = """
            FROM public.android_push_subscription s
            JOIN public.api_login_token t ON t.id = s.api_login_token_id
            JOIN public.users u ON u.id = t.user_id
            JOIN public.safety_dispatch_recipient r ON r.user_id = u.id
            JOIN public.safety_dispatch d ON d.id = r.dispatch_id
            WHERE r.dispatch_id = ?
              AND u.active = TRUE
              AND u.notification_enabled = TRUE
              AND u.role IN ('STUDENT', 'FAMILY')
              AND (d.deadline_at IS NULL OR u.role = 'STUDENT')
              AND t.expires_at > CURRENT_TIMESTAMP
            """;

    public List<FamilyNotificationTarget> findDispatchTargets(long dispatchId)
            throws NamingException, SQLException {
        String sql = "SELECT s.id, s.api_login_token_id, s.fcm_token "
                + DISPATCH_TARGETS + " ORDER BY s.id";
        List<FamilyNotificationTarget> targets = new ArrayList<>();
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, dispatchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    targets.add(new FamilyNotificationTarget(rs.getLong("id"),
                            rs.getLong("api_login_token_id"), rs.getString("fcm_token")));
                }
            }
        }
        return targets;
    }

    /** 送信直前に配信対象、ログイン、通知設定を再確認する。 */
    public boolean isDispatchTargetEnabled(long dispatchId, FamilyNotificationTarget target)
            throws NamingException, SQLException {
        String sql = "SELECT 1 " + DISPATCH_TARGETS
                + " AND s.id = ? AND s.api_login_token_id = ? AND s.fcm_token = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, dispatchId);
            ps.setLong(2, target.getSubscriptionId());
            ps.setLong(3, target.getLoginTokenId());
            ps.setString(4, target.getFcmToken());
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /**
     * 通知先の取得時点の情報。
     * トークンをログへ出力しない。
     */
    public static final class FamilyNotificationTarget {

        private final long subscriptionId;
        private final long loginTokenId;
        private final String fcmToken;

        public FamilyNotificationTarget(
                long subscriptionId,
                long loginTokenId,
                String fcmToken) {

            this.subscriptionId = subscriptionId;
            this.loginTokenId = loginTokenId;
            this.fcmToken = fcmToken;
        }

        public long getSubscriptionId() {
            return subscriptionId;
        }

        public long getLoginTokenId() {
            return loginTokenId;
        }

        public String getFcmToken() {
            return fcmToken;
        }
    }
}