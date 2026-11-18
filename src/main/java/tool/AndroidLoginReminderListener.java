package tool;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Androidアプリのログイン期限通知を定期実行する。
 */
@WebListener
public class AndroidLoginReminderListener
        implements ServletContextListener {

    private static final Logger LOGGER =
            Logger.getLogger(
                    AndroidLoginReminderListener.class.getName());

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(
            ServletContextEvent event) {

        // 明示的に有効化された環境だけで実行する。
        if (!"true".equalsIgnoreCase(
                System.getenv("ANDROID_LOGIN_REMINDER_ENABLED"))) {

            LOGGER.info(
                    "Androidログイン期限通知の定期処理は無効です。");
            return;
        }

        scheduler =
                Executors.newSingleThreadScheduledExecutor(
                        task -> {
                            Thread thread = new Thread(
                                    task,
                                    "anpilink-android-login-reminder");

                            thread.setDaemon(true);
                            return thread;
                        });

        // 起動60秒後に開始し、処理終了から1時間後に再確認する。
        scheduler.scheduleWithFixedDelay(
                this::sendReminders,
                60,
                3600,
                TimeUnit.SECONDS);

        LOGGER.info(
                "Androidログイン期限通知の定期処理を開始しました。");
    }

    private void sendReminders() {

        try {
            new AndroidLoginReminderService()
                    .sendDueReminders();

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

        } catch (Exception e) {

            // 認証情報や通知先はログに出力しない。
            LOGGER.log(
                    Level.WARNING,
                    "Androidログイン期限通知の定期処理失敗: 例外={0}",
                    e.getClass().getSimpleName());
        }
    }

    @Override
    public void contextDestroyed(
            ServletContextEvent event) {

        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}