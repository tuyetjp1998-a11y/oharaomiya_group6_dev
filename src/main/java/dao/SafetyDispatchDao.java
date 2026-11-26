package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.naming.NamingException;

/** 校長の安否確認配信を管理するDAO。 */
public class SafetyDispatchDao extends DAO {

    /** 配信内容と配信対象者を同一トランザクションで登録する。 */
    public long create(long senderUserId, String targetType,
            String title, String message, OffsetDateTime deadline)
            throws NamingException, SQLException {

        if (senderUserId <= 0 || targetType == null
                || !(targetType.equals("STUDENT")
                    || targetType.equals("FAMILY")
                    || targetType.equals("TEACHER")
                    || targetType.equals("ALL"))
                || title == null || title.isBlank() || title.length() > 100
                || message == null || message.isBlank() || message.length() > 2000
                || (deadline != null && (!"STUDENT".equals(targetType)
                    || !deadline.isAfter(OffsetDateTime.now())))) {
            throw new IllegalArgumentException("配信内容が正しくありません。");
        }

        String insertDispatch = """
                INSERT INTO public.safety_dispatch
                    (sender_user_id, target_type, title, message, deadline_at)
                SELECT id, ?, ?, ?, ?
                FROM public.users
                WHERE id = ? AND role = 'PRINCIPAL' AND active = TRUE
                RETURNING id
                """;

        String insertRecipients = """
                INSERT INTO public.safety_dispatch_recipient
                    (dispatch_id, user_id)
                SELECT ?, id
                FROM public.users
                WHERE active = TRUE
                  AND ((? = 'ALL' AND role IN ('STUDENT', 'FAMILY', 'TEACHER'))
                       OR role = ?)
                """;

        try (Connection connection = getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                long dispatchId;
                try (PreparedStatement statement = connection.prepareStatement(insertDispatch)) {
                    statement.setString(1, targetType);
                    statement.setString(2, title.strip());
                    statement.setString(3, message.strip());
                    statement.setObject(4, deadline);
                    statement.setLong(5, senderUserId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            throw new SecurityException("配信権限がありません。");
                        }
                        dispatchId = rs.getLong("id");
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(insertRecipients)) {
                    statement.setLong(1, dispatchId);
                    statement.setString(2, targetType);
                    statement.setString(3, targetType);
                    if (statement.executeUpdate() == 0) {
                        throw new IllegalStateException("配信対象者がいません。");
                    }
                }

                connection.commit();
                connection.setAutoCommit(originalAutoCommit);
                return dispatchId;
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                    // ロールバック失敗時は自動コミットへ戻さず接続を破棄する。
                    throw e;
                }
                connection.setAutoCommit(originalAutoCommit);
                throw e;
            }
        }
    }

    /** 本人宛ての配信と回答状況を取得する。 */
    public List<DispatchNotice> findByStudent(long studentUserId)
            throws NamingException, SQLException {
        String sql = """
                SELECT d.id, d.title, d.message, d.deadline_at, d.dispatched_at,
                       r.read_at,
                       EXISTS (SELECT 1 FROM public.safety_report sr
                               WHERE sr.dispatch_id = d.id AND sr.user_id = r.user_id
                                 AND NOT EXISTS (SELECT 1 FROM public.safety_report_cancellation c
                                                 WHERE c.report_id = sr.id)) AS answered
                FROM public.safety_dispatch_recipient r
                JOIN public.safety_dispatch d ON d.id = r.dispatch_id
                JOIN public.users u ON u.id = r.user_id
                WHERE r.user_id = ? AND u.active = TRUE AND u.role = 'STUDENT'
                ORDER BY d.dispatched_at DESC, d.id DESC
                """;
        List<DispatchNotice> notices = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    notices.add(new DispatchNotice(rs.getLong("id"), rs.getString("title"),
                            rs.getString("message"), rs.getObject("deadline_at", OffsetDateTime.class),
                            rs.getObject("dispatched_at", OffsetDateTime.class),
                            rs.getObject("read_at", OffsetDateTime.class), rs.getBoolean("answered")));
                }
            }
        }
        return notices;
    }


    /** 本人宛ての配信を既読にする。初回の既読日時を保持する。 */
    public boolean markRead(long userId, long dispatchId, String role)
            throws NamingException, SQLException {
        if (userId <= 0 || dispatchId <= 0 || role == null
                || !List.of("STUDENT", "FAMILY", "TEACHER").contains(role)) {
            return false;
        }
        String sql = """
                UPDATE public.safety_dispatch_recipient AS r
                SET read_at = COALESCE(r.read_at, CURRENT_TIMESTAMP)
                FROM public.safety_dispatch d, public.users u
                WHERE r.dispatch_id = d.id AND r.user_id = u.id
                  AND r.dispatch_id = ? AND r.user_id = ?
                  AND u.active = TRUE AND u.role = ?
                  AND (u.role = 'STUDENT' OR d.deadline_at IS NULL)
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, dispatchId);
            statement.setLong(2, userId);
            statement.setString(3, role);
            return statement.executeUpdate() == 1;
        }
    }

    public record DispatchNotice(long dispatchId, String title, String message,
            OffsetDateTime deadlineAt, OffsetDateTime dispatchedAt,
            OffsetDateTime readAt, boolean answered) {}
}
