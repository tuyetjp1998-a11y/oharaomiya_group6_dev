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
    	               location_enabled
    	        FROM public.users
    	        WHERE login_id = ?
    	        """;

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

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
                
                // パスワードハッシュはUserとは別に保持する
                String passwordHash =
                        resultSet.getString("password_hash");

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
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

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
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setBoolean(1, enabled);
            statement.setLong(2, userId);

            return statement.executeUpdate() == 1;
        }
    }
    /**
     * ログイン処理で使用するデータ。
     * このオブジェクトはセッションに保存しない。
     */
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