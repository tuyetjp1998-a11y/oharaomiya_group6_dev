package tool;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import dao.AndroidLoginReminderDao;
import dao.AndroidLoginReminderDao.ReminderTarget;

/**
 * Androidアプリへログイン期限の通知を送信する。
 */
public class AndroidLoginReminderService {

    private static final Logger LOGGER =
            Logger.getLogger(
                    AndroidLoginReminderService.class.getName());

    private static final ZoneId JAPAN_ZONE =
            ZoneId.of("Asia/Tokyo");

    /**
     * 日本時間で各通知登録につき1日1回まで送信を試みる。
     */
    public void sendDueReminders() throws Exception {

        AndroidLoginReminderDao dao =
                new AndroidLoginReminderDao();

        LocalDate reminderDate =
                LocalDate.now(JAPAN_ZONE);

        List<ReminderTarget> targets =
                dao.findTargets(reminderDate);

        if (targets.isEmpty()) {
            LOGGER.info(
                    "Androidログイン期限通知: 対象なし");
            return;
        }

        // 認証ファイルの読み込みなどは送信権の取得前に行う。
        FcmSender sender = new FcmSender();

        for (ReminderTarget target : targets) {

            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            try {
                LocalDate sendDate =
                        LocalDate.now(JAPAN_ZONE);

                // 送信前に当日の処理権を取得する。
                // 送信失敗時も、当日は再送しない。
                if (!dao.claim(target, sendDate)) {
                    continue;
                }

                FcmSender.SendResult result = sender.send(
                        target.getFcmToken(),
                        "AnpiLink ログイン期限のお知らせ",
                        "ログインの有効期限が近づいています。"
                                + "アプリを開いて再ログインしてください。");

                if (result.isAccepted()) {

                    // FCMによる受付成功。端末への到達確認ではない。
                    LOGGER.log(
                            Level.INFO,
                            "Androidログイン期限通知受付成功: "
                                    + "登録ID={0}, HTTP={1}",
                            new Object[] {
                                    target.getId(),
                                    result.getStatusCode()
                            });

                } else if (result.isUnregistered()) {

                    // 現在も同じ登録の場合だけ削除する。
                    boolean deleted =
                            dao.deleteUnregistered(target);

                    LOGGER.log(
                            Level.INFO,
                            "Android通知登録失効: "
                                    + "登録ID={0}, 削除={1}",
                            new Object[] {
                                    target.getId(),
                                    deleted
                            });

                } else {

                    LOGGER.log(
                            Level.WARNING,
                            "Androidログイン期限通知受付失敗: "
                                    + "登録ID={0}, HTTP={1}, エラー={2}",
                            new Object[] {
                                    target.getId(),
                                    result.getStatusCode(),
                                    result.getErrorCode()
                            });
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
                return;

            } catch (Exception e) {

                // トークン・認証鍵・レスポンス本文は出力しない。
                LOGGER.log(
                        Level.WARNING,
                        "Androidログイン期限通知処理失敗: "
                                + "登録ID={0}, 例外={1}",
                        new Object[] {
                                target.getId(),
                                e.getClass().getSimpleName()
                        });
            }
        }
    }
}