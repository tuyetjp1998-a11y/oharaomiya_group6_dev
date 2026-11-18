package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.naming.NamingException;

/**
 * 保護者の安否回答通知設定を更新する。
 */
public class FamilyNotificationSettingDao extends DAO {

    /**
     * 有効な保護者本人の通知設定を更新する。
     */
    public boolean update(
            long familyUserId,
            boolean notificationEnabled)
            throws NamingException, SQLException {

        String sql = """
                UPDATE public.users
                SET notification_enabled = ?
                WHERE id = ?
                  AND role = 'FAMILY'
                  AND active = TRUE
                """;

        try (Connection connection = getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setBoolean(1, notificationEnabled);
            statement.setLong(2, familyUserId);

            return statement.executeUpdate() == 1;
        }
    }
}