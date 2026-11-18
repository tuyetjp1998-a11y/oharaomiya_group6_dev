package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

import javax.naming.NamingException;

import bean.User;

/**
 * Androidアプリの認証トークンを管理する。
 */
public class ApiLoginTokenDao extends DAO {

    /**
     * 有効な生徒・保護者アカウントのトークンを保存する。
     * トークンの平文は保存しない。
     *
     * @return 登録されたID。対象アカウントが無効な場合はnull
     */
    public Long saveAndReturnId(
            long userId,
            String accessTokenHash,
            String refreshTokenHash,
            OffsetDateTime accessExpiresAt,
            OffsetDateTime expiresAt)
            throws NamingException, SQLException {

        String sql = """
                INSERT INTO public.api_login_token
                    (user_id,
                     access_token_hash,
                     refresh_token_hash,
                     access_expires_at,
                     expires_at)
                SELECT id, ?, ?, ?, ?
                FROM public.users
                WHERE id = ?
                  AND role IN ('STUDENT', 'FAMILY')
                  AND active = TRUE
                RETURNING id
                """;

        try (Connection connection = getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setString(1, accessTokenHash);
            statement.setString(2, refreshTokenHash);
            statement.setObject(3, accessExpiresAt);
            statement.setObject(4, expiresAt);
            statement.setLong(5, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? resultSet.getLong("id")
                        : null;
            }
        }
    }
    /**
     * 有効なアクセストークンから最新の利用者情報を取得する。
     * トークンの更新や有効期限の延長は行わない。
     */
    public User findByAccessTokenHash(String accessTokenHash)
            throws NamingException, SQLException {

        String sql = """
                SELECT u.id,
                       u.login_id,
                       u.name,
                       u.role,
                       u.active,
                       u.created_at,
                       u.location_enabled,
                       u.notification_enabled
                FROM public.api_login_token t
                JOIN public.users u ON u.id = t.user_id
                WHERE t.access_token_hash = ?
                  AND t.access_expires_at > CURRENT_TIMESTAMP
                  AND t.expires_at > CURRENT_TIMESTAMP
                  AND u.active = TRUE
                  AND u.role IN ('STUDENT', 'FAMILY')
                """;

        try (Connection connection = getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setString(1, accessTokenHash);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

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

                return user;
            }
        }
    }
    /**
     * リフレッシュトークンを確認し、
     * 新しいアクセストークンのハッシュを保存する。
     * ログインの有効期限は延長しない。
     */
    public RefreshData refreshAccessToken(
            String refreshTokenHash,
            String newAccessTokenHash)
            throws NamingException, SQLException {

        String sql = """
                UPDATE public.api_login_token t
                SET access_token_hash = ?,
                    access_expires_at = LEAST(
                        CURRENT_TIMESTAMP + INTERVAL '15 minutes',
                        t.expires_at
                    )
                FROM public.users u
                WHERE t.refresh_token_hash = ?
                  AND t.expires_at > CURRENT_TIMESTAMP
                  AND u.id = t.user_id
                  AND u.active = TRUE
                  AND u.role IN ('STUDENT', 'FAMILY')
                RETURNING t.access_expires_at,
                          t.expires_at
                """;

        try (Connection connection = getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setString(1, newAccessTokenHash);
            statement.setString(2, refreshTokenHash);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                return new RefreshData(
                        resultSet.getObject(
                                "access_expires_at",
                                OffsetDateTime.class),
                        resultSet.getObject(
                                "expires_at",
                                OffsetDateTime.class));
            }
        }
    }

    /**
     * アクセストークン更新後の有効期限。
     */
    public static final class RefreshData {

        private final OffsetDateTime accessExpiresAt;
        private final OffsetDateTime refreshExpiresAt;

        public RefreshData(
                OffsetDateTime accessExpiresAt,
                OffsetDateTime refreshExpiresAt) {

            this.accessExpiresAt = accessExpiresAt;
            this.refreshExpiresAt = refreshExpiresAt;
        }

        public OffsetDateTime getAccessExpiresAt() {
            return accessExpiresAt;
        }

        public OffsetDateTime getRefreshExpiresAt() {
            return refreshExpiresAt;
        }
    }
    /**
     * 指定されたログインのトークンを削除する。
     * アクセストークンとリフレッシュトークンを同時に無効化する。
     */
    public boolean deleteByRefreshTokenHash(
            String refreshTokenHash)
            throws NamingException, SQLException {

        String sql = """
                DELETE FROM public.api_login_token
                WHERE refresh_token_hash = ?
                """;

        try (Connection connection = getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setString(1, refreshTokenHash);

            return statement.executeUpdate() > 0;
        }
    }
}