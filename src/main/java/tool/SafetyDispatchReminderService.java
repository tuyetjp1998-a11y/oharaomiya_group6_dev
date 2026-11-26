package tool;

import dao.SafetyDispatchReminderDao;
import java.util.logging.Logger;

/** 回答期限を確認して保護者向け通知を作成・送信する。 */
public final class SafetyDispatchReminderService implements Runnable {
    private static final Logger LOG = Logger.getLogger(SafetyDispatchReminderService.class.getName());
    private long cursor = 0;

    @Override
    public void run() {
        try {
            var dao = new SafetyDispatchReminderDao();
            int created = dao.createOverdueReminders();
            if (created > 0) LOG.info("催促通知登録件数=" + created);
            var ids = dao.findPendingDeliveryIds(cursor);
            if (ids.isEmpty()) { cursor = 0; return; }
            var sender = new FcmSender();
            for (long id : ids) {
                if (Thread.currentThread().isInterrupted()) return;
                try {
                    if (dao.deliverPending(id, sender)) LOG.info("催促通知FCM受付成功: ID=" + id);
                } catch (Exception e) {
                    LOG.warning("催促通知処理失敗: " + e.getClass().getSimpleName());
                }
                cursor = id;
            }
            if (ids.size() < 20) cursor = 0;
        } catch (Exception e) {
            LOG.warning("催促通知定期処理失敗: " + e.getClass().getSimpleName());
        }
    }
}
