package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.naming.NamingException;

/** 教員本人宛ての通常通知を取得する。 */
public class TeacherNoticeDao extends DAO {
    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                    .withZone(ZoneId.of("Asia/Tokyo"));

    public List<Map<String, Object>> findByTeacher(long teacherId)
            throws NamingException, SQLException {
        return find(teacherId, null);
    }

    public Map<String, Object> findDetail(long teacherId, long dispatchId)
            throws NamingException, SQLException {
        var rows = find(teacherId, dispatchId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<Map<String, Object>> find(long teacherId, Long dispatchId)
            throws NamingException, SQLException {
        String sql = """
                SELECT d.id, d.title, d.message, d.dispatched_at, r.read_at
                FROM public.safety_dispatch_recipient r
                JOIN public.safety_dispatch d ON d.id = r.dispatch_id
                JOIN public.users u ON u.id = r.user_id
                WHERE r.user_id = ? AND u.role = 'TEACHER' AND u.active = TRUE
                  AND d.deadline_at IS NULL
                """ + (dispatchId == null ? "" : " AND d.id = ? ")
                + " ORDER BY d.dispatched_at DESC, d.id DESC";
        var rows = new ArrayList<Map<String, Object>>();
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, teacherId);
            if (dispatchId != null) ps.setLong(2, dispatchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(Map.of(
                            "dispatchId", rs.getLong("id"),
                            "title", rs.getString("title"),
                            "message", rs.getString("message"),
                            "unread", rs.getObject("read_at") == null,
                            "displayTime", DISPLAY_TIME.format(
                                    rs.getObject("dispatched_at", OffsetDateTime.class))));
                }
            }
        }
        return rows;
    }
}
