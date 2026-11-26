package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.naming.NamingException;

/** 保護者本人宛ての通常通知を取得する。 */
public class FamilyDispatchNoticeDao extends DAO {
    public List<Map<String, Object>> findByFamily(long familyUserId)
            throws NamingException, SQLException {
        String sql = """
                SELECT d.id, d.title, d.message, d.dispatched_at, r.read_at
                FROM public.safety_dispatch_recipient r
                JOIN public.safety_dispatch d ON d.id = r.dispatch_id
                JOIN public.users u ON u.id = r.user_id
                WHERE r.user_id = ? AND u.role = 'FAMILY' AND u.active = TRUE
                  AND d.deadline_at IS NULL
                ORDER BY d.dispatched_at DESC, d.id DESC
                """;
        List<Map<String, Object>> notices = new ArrayList<>();
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, familyUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new java.util.LinkedHashMap<>();
                    item.put("dispatchId", rs.getLong("id"));
                    item.put("title", rs.getString("title"));
                    item.put("message", rs.getString("message"));
                    item.put("createdAt", rs.getObject("dispatched_at", OffsetDateTime.class).toString());
                    OffsetDateTime readAt = rs.getObject("read_at", OffsetDateTime.class);
                    item.put("readAt", readAt == null ? null : readAt.toString());
                    notices.add(item);
                }
            }
        }
        return notices;
    }
}
