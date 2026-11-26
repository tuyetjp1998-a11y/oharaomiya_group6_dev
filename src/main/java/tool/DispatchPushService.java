package tool;

import dao.AndroidPushSubscriptionDao;
import java.util.logging.Logger;

/** 保存済み配信のAndroid通知。失敗しても配信データは取り消さない。 */
public final class DispatchPushService {
    private static final Logger LOG = Logger.getLogger(DispatchPushService.class.getName());
    private DispatchPushService() {}

    public static void send(long dispatchId, boolean safety) {
        try {
            var dao = new AndroidPushSubscriptionDao();
            var targets = dao.findDispatchTargets(dispatchId);
            if (targets.isEmpty()) return;
            var sender = new FcmSender();
            // ロック画面に学校の連絡本文や個人情報を出さない。
            String title = safety ? "安否確認通知" : "学校からの連絡";
            String body = safety ? "アプリを開いて、安否状況を回答してください。"
                    : "学校から新しい連絡が届きました。アプリで確認してください。";
            for (var target : targets) {
                if (Thread.currentThread().isInterrupted()) return;
                try {
                    if (!dao.isDispatchTargetEnabled(dispatchId, target)) continue;
                    var result = sender.send(target.getFcmToken(), title, body);
                    if (result.isUnregistered()) {
                        dao.deleteUnregisteredFamilyTarget(target);
                    }
                    LOG.info("配信FCM: dispatchId=" + dispatchId + ", HTTP=" + result.getStatusCode());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    LOG.warning("配信FCM送信失敗: " + e.getClass().getSimpleName());
                }
            }
        } catch (Exception e) {
            LOG.warning("配信FCM処理失敗: " + e.getClass().getSimpleName());
        }
    }
}
