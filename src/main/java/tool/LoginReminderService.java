package tool;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import bean.WebPushSubscription;
import dao.WebPushSubscriptionDao;

/**
 * 生徒・保護者へログイン期限の通知を送信する。
 */
public class LoginReminderService {

    private static final Logger LOGGER =
            Logger.getLogger(
                    LoginReminderService.class.getName());

    private static final ZoneId JAPAN_ZONE =
            ZoneId.of("Asia/Tokyo");

    /**
     * 通知対象を取得し、各登録につき当日1回まで送信を試みる。
     */
    public void sendDueReminders() throws Exception {

        WebPushSubscriptionDao dao =
                new WebPushSubscriptionDao();

        LocalDate reminderDate =
                LocalDate.now(JAPAN_ZONE);

        List<WebPushSubscription> subscriptions =
                dao.findLoginReminderTargets(reminderDate);

        if (subscriptions.isEmpty()) {
            LOGGER.info(
                    "ログイン期限通知: 対象なし");
            return;
        }

        // 設定に問題がある場合は、送信権を取得する前に終了する
        WebPushSender sender = new WebPushSender();

        for (WebPushSubscription subscription : subscriptions) {

            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            try {
                // 日付が変わった場合も、日本時間の当日で判定する
                LocalDate sendDate =
                        LocalDate.now(JAPAN_ZONE);

                boolean claimed = dao.claimLoginReminder(
                        subscription,
                        sendDate);

                if (!claimed) {
                    continue;
                }

                int statusCode =
                        sender.sendLoginReminder(subscription);

                if (statusCode >= 200
                        && statusCode < 300) {

                    // Pushサービスの受付であり、端末への到達確認ではない
                    LOGGER.log(
                            Level.INFO,
                            "ログイン期限通知受付成功: 登録ID={0}",
                            subscription.getId());

                } else if (statusCode == 404
                        || statusCode == 410) {

                    // 使用できなくなった通知登録を無効化する
                    boolean marked =
                            dao.markExpired(subscription);

                    LOGGER.log(
                            Level.INFO,
                            "ログイン期限通知の登録失効: "
                                    + "登録ID={0}, 更新={1}",
                            new Object[] {
                                    subscription.getId(),
                                    marked
                            });

                } else {

                    LOGGER.log(
                            Level.WARNING,
                            "ログイン期限通知受付失敗: "
                                    + "登録ID={0}, HTTP={1}",
                            new Object[] {
                                    subscription.getId(),
                                    statusCode
                            });
                }

            } catch (InterruptedException e) {

                // 停止要求を維持して処理を終了する
                Thread.currentThread().interrupt();
                return;

            } catch (Exception e) {

                // endpoint・鍵・トークンはログに出力しない
                LOGGER.log(
                        Level.WARNING,
                        "ログイン期限通知処理失敗: "
                                + "登録ID={0}, 例外={1}",
                        new Object[] {
                                subscription.getId(),
                                e.getClass().getSimpleName()
                        });
            }
        }
    }
}