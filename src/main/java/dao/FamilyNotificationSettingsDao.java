package dao;

import java.sql.SQLException;
import javax.naming.NamingException;

public class FamilyNotificationSettingsDao extends DAO {
    public record Settings(boolean answerEnabled, boolean schoolEnabled) {}

    public Settings get(long userId) throws NamingException, SQLException {
        String sql = """
                SELECT notification_enabled, school_notification_enabled
                FROM public.users WHERE id = ? AND role = 'FAMILY' AND active
                """;
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (var rows = statement.executeQuery()) {
                return rows.next() ? new Settings(rows.getBoolean(1), rows.getBoolean(2)) : null;
            }
        }
    }

    public boolean update(long userId, boolean school, boolean enabled)
            throws NamingException, SQLException {
        String column = school ? "school_notification_enabled" : "notification_enabled";
        String sql = "UPDATE public.users SET " + column
                + " = ? WHERE id = ? AND role = 'FAMILY' AND active";
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, enabled);
            statement.setLong(2, userId);
            return statement.executeUpdate() == 1;
        }
    }
}
