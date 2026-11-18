package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.NamingException;

/**
 * 生徒の位置情報・通知設定を管理する。
 */
public class StudentSettingsDao extends DAO {

    /**
     * 有効な生徒アカウントの設定を取得する。
     * 対象が存在しない場合はnullを返す。
     */
    public SettingsData getSettings(long userId)
            throws NamingException, SQLException {

        String sql = """
                SELECT location_enabled,
                       notification_enabled
                FROM public.users
                WHERE id = ?
                  AND role = 'STUDENT'
                  AND active = TRUE
                """;

        try (Connection connection = getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setLong(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new SettingsData(
                        resultSet.getBoolean("location_enabled"),
                        resultSet.getBoolean("notification_enabled"));
            }
        }
    }

    /**
     * 位置情報の利用設定だけを更新する。
     */
    public boolean updateLocationEnabled(
            long userId,
            boolean enabled)
            throws NamingException, SQLException {

        String sql = """
                UPDATE public.users
                SET location_enabled = ?
                WHERE id = ?
                  AND role = 'STUDENT'
                  AND active = TRUE
                """;

        return updateSetting(sql, userId, enabled);
    }

    /**
     * 通知設定だけを更新する。
     */
    public boolean updateNotificationEnabled(
            long userId,
            boolean enabled)
            throws NamingException, SQLException {

        String sql = """
                UPDATE public.users
                SET notification_enabled = ?
                WHERE id = ?
                  AND role = 'STUDENT'
                  AND active = TRUE
                """;

        return updateSetting(sql, userId, enabled);
    }

    /**
     * 固定のSQLで指定された設定を更新する。
     */
    private boolean updateSetting(
            String sql,
            long userId,
            boolean enabled)
            throws NamingException, SQLException {

        try (Connection connection = getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setBoolean(1, enabled);
            statement.setLong(2, userId);

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * 生徒の設定情報。
     */
    public static final class SettingsData {

        private final boolean locationEnabled;
        private final boolean notificationEnabled;

        public SettingsData(
                boolean locationEnabled,
                boolean notificationEnabled) {

            this.locationEnabled = locationEnabled;
            this.notificationEnabled = notificationEnabled;
        }

        public boolean isLocationEnabled() {
            return locationEnabled;
        }

        public boolean isNotificationEnabled() {
            return notificationEnabled;
        }
    }
}