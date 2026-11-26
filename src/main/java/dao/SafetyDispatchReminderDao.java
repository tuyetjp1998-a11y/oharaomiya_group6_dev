package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.NamingException;

/** 安否確認の回答期限超過に伴う保護者向け催促通知を管理する。 */
public class SafetyDispatchReminderDao extends DAO {

    /**
     * 期限超過かつ未回答の生徒について、関連付け済みの保護者への通知を登録する。
     * 同一配信・生徒・保護者の組み合わせは重複登録しない。
     * @return 新規登録件数
     */
    public int createOverdueReminders() throws NamingException, SQLException {
        String sql = """
                INSERT INTO public.safety_dispatch_reminder
                    (dispatch_id, student_user_id, family_user_id)
                SELECT d.id, student.id, family.id
                FROM public.safety_dispatch AS d
                JOIN public.safety_dispatch_recipient AS recipient
                  ON recipient.dispatch_id = d.id
                JOIN public.users AS student
                  ON student.id = recipient.user_id
                 AND student.role = 'STUDENT'
                 AND student.active = TRUE
                JOIN public.family_student AS fs
                  ON fs.student_user_id = student.id
                JOIN public.users AS family
                  ON family.id = fs.family_user_id
                 AND family.role = 'FAMILY'
                 AND family.active = TRUE
                WHERE d.deadline_at IS NOT NULL
                  AND d.deadline_at <= CURRENT_TIMESTAMP
                  AND NOT EXISTS (
                      SELECT 1
                      FROM public.safety_report AS report
                      WHERE report.dispatch_id = d.id
                        AND report.user_id = student.id
                        AND NOT EXISTS (
                            SELECT 1
                            FROM public.safety_report_cancellation AS cancellation
                            WHERE cancellation.report_id = report.id
                        )
                  )
                ON CONFLICT (dispatch_id, student_user_id, family_user_id)
                DO NOTHING
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        }
    }

    /** 未送信の催促通知件数を取得する。 */
    public long countPendingReminders() throws NamingException, SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM public.safety_dispatch_reminder
                WHERE sent_at IS NULL
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    /** ログイン中の保護者に関連する催促通知だけ取得する。 */
    public java.util.List<java.util.Map<String, Object>> findByFamily(long familyUserId)
            throws NamingException, SQLException {
        String sql = """
                SELECT r.id, r.dispatch_id, student.name AS student_name,
                       d.title, d.deadline_at, r.created_at, r.read_at
                FROM public.safety_dispatch_reminder r
                JOIN public.safety_dispatch d ON d.id = r.dispatch_id
                JOIN public.users student ON student.id = r.student_user_id
                    AND student.active = TRUE AND student.role = 'STUDENT'
                JOIN public.users family ON family.id = r.family_user_id
                    AND family.active = TRUE AND family.role = 'FAMILY'
                JOIN public.family_student fs
                    ON fs.family_user_id = r.family_user_id
                   AND fs.student_user_id = r.student_user_id
                WHERE r.family_user_id = ?
                ORDER BY r.created_at DESC, r.id DESC
                """;
        var notices = new java.util.ArrayList<java.util.Map<String, Object>>();
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, familyUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    var item = new java.util.LinkedHashMap<String, Object>();
                    item.put("reminderId", rs.getLong("id"));
                    item.put("dispatchId", rs.getLong("dispatch_id"));
                    item.put("studentName", rs.getString("student_name"));
                    item.put("title", rs.getString("title"));
                    item.put("deadlineAt", rs.getObject("deadline_at", java.time.OffsetDateTime.class).toString());
                    item.put("createdAt", rs.getObject("created_at", java.time.OffsetDateTime.class).toString());
                    var readAt = rs.getObject("read_at", java.time.OffsetDateTime.class);
                    item.put("readAt", readAt == null ? null : readAt.toString());
                    notices.add(item);
                }
            }
        }
        return notices;
    }

    // 現在も未回答であり、保護者との関連付けが有効な通知だけを送信対象にする。
    private static final String ELIGIBLE = """
            FROM public.safety_dispatch_reminder r
            JOIN public.family_student fs ON fs.family_user_id = r.family_user_id
                AND fs.student_user_id = r.student_user_id
            JOIN public.users f ON f.id = r.family_user_id
                AND f.active = TRUE AND f.role = 'FAMILY'
                AND f.notification_enabled = TRUE
            JOIN public.users st ON st.id = r.student_user_id
                AND st.active = TRUE AND st.role = 'STUDENT'
            WHERE r.sent_at IS NULL
              AND NOT EXISTS (
                  SELECT 1 FROM public.safety_report a
                  WHERE a.dispatch_id = r.dispatch_id AND a.user_id = r.student_user_id
                    AND NOT EXISTS (SELECT 1 FROM public.safety_report_cancellation c
                                    WHERE c.report_id = a.id)
              )
            """;

    private static final String TARGETS = """
            SELECT s.id, s.api_login_token_id, s.fcm_token
            FROM public.android_push_subscription s
            JOIN public.api_login_token t ON t.id = s.api_login_token_id
            WHERE t.user_id = ? AND t.expires_at > CURRENT_TIMESTAMP
            ORDER BY s.id
            """;

    /** 送信可能な通知を取得する。端末未登録の通知は一覧表示用に残す。 */
    public java.util.List<Long> findPendingDeliveryIds(long afterId)
            throws NamingException, SQLException {
        String sql = "SELECT r.id " + ELIGIBLE + """
                AND r.id > ?
                AND EXISTS (SELECT 1 FROM public.android_push_subscription s
                            JOIN public.api_login_token t ON t.id = s.api_login_token_id
                            WHERE t.user_id = r.family_user_id
                              AND t.expires_at > CURRENT_TIMESTAMP)
                ORDER BY r.id LIMIT 20
                """;
        var ids = new java.util.ArrayList<Long>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, afterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getLong(1));
            }
        }
        return ids;
    }

    /** 同一通知を並行送信しない。FCM受付成功時だけ sent_at を記録する。 */
    public boolean deliverPending(long reminderId, tool.FcmSender sender)
            throws Exception {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                long familyId;
                String lockSql = "SELECT r.family_user_id " + ELIGIBLE
                        + " AND r.id = ? FOR UPDATE OF r SKIP LOCKED";
                try (PreparedStatement ps = c.prepareStatement(lockSql)) {
                    ps.setLong(1, reminderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) { c.commit(); return false; }
                        familyId = rs.getLong(1);
                    }
                }
                var targets = new java.util.ArrayList<AndroidPushSubscriptionDao.FamilyNotificationTarget>();
                try (PreparedStatement ps = c.prepareStatement(TARGETS)) {
                    ps.setLong(1, familyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) targets.add(new AndroidPushSubscriptionDao.FamilyNotificationTarget(
                                rs.getLong("id"), rs.getLong("api_login_token_id"), rs.getString("fcm_token")));
                    }
                }
                boolean accepted = false;
                String recheck = "SELECT 1 " + ELIGIBLE + """
                        AND r.id = ? AND r.family_user_id = ?
                        AND EXISTS (SELECT 1 FROM public.android_push_subscription s
                            JOIN public.api_login_token t ON t.id = s.api_login_token_id
                            WHERE s.id = ? AND s.api_login_token_id = ? AND s.fcm_token = ?
                              AND t.user_id = r.family_user_id AND t.expires_at > CURRENT_TIMESTAMP)
                        """;
                for (var target : targets) {
                    if (Thread.currentThread().isInterrupted()) break;
                    try (PreparedStatement ps = c.prepareStatement(recheck)) {
                        ps.setLong(1, reminderId); ps.setLong(2, familyId);
                        ps.setLong(3, target.getSubscriptionId()); ps.setLong(4, target.getLoginTokenId());
                        ps.setString(5, target.getFcmToken());
                        try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) continue; }
                    }
                    try {
                        var result = sender.send(target.getFcmToken(), "AnpiLink",
                                "お子様の安否確認が未回答です。アプリで確認してください。");
                        if (result.isAccepted()) accepted = true;
                        else if (result.isUnregistered()) {
                            try (PreparedStatement ps = c.prepareStatement("""
                                    DELETE FROM public.android_push_subscription
                                    WHERE id = ? AND api_login_token_id = ? AND fcm_token = ?
                                    """)) {
                                ps.setLong(1, target.getSubscriptionId());
                                ps.setLong(2, target.getLoginTokenId()); ps.setString(3, target.getFcmToken());
                                ps.executeUpdate();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); break;
                    } catch (java.io.IOException | java.security.GeneralSecurityException e) {
                        java.util.logging.Logger.getLogger(getClass().getName()).warning(
                                "催促通知送信失敗: " + e.getClass().getSimpleName());
                    }
                }
                if (accepted) {
                    try (PreparedStatement ps = c.prepareStatement("""
                            UPDATE public.safety_dispatch_reminder SET sent_at = CURRENT_TIMESTAMP
                            WHERE id = ? AND sent_at IS NULL
                            """)) {
                        ps.setLong(1, reminderId); ps.executeUpdate();
                    }
                }
                c.commit();
                return accepted;
            } catch (Exception e) {
                try { c.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
                throw e;
            }
        }
    }


    /** 本人宛ての催促通知を既読にする。現在の親子関係も確認する。 */
    public boolean markRead(long familyUserId, long reminderId)
            throws NamingException, SQLException {
        if (familyUserId <= 0 || reminderId <= 0) return false;
        String sql = """
                UPDATE public.safety_dispatch_reminder AS r
                SET read_at = COALESCE(r.read_at, CURRENT_TIMESTAMP)
                FROM public.users family, public.users student,
                     public.family_student fs, public.safety_dispatch d
                WHERE r.id = ? AND r.family_user_id = ?
                  AND family.id = r.family_user_id
                  AND family.role = 'FAMILY' AND family.active = TRUE
                  AND student.id = r.student_user_id
                  AND student.role = 'STUDENT' AND student.active = TRUE
                  AND fs.family_user_id = r.family_user_id
                  AND fs.student_user_id = r.student_user_id
                  AND d.id = r.dispatch_id AND d.deadline_at IS NOT NULL
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reminderId);
            statement.setLong(2, familyUserId);
            return statement.executeUpdate() == 1;
        }
    }

}
