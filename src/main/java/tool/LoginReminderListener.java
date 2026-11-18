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
 * ログイン期限通知の定期処理を管理する。
 */
@WebListener
public class LoginReminderListener
        implements ServletContextListener {

    private static final Logger LOGGER =
            Logger.getLogger(
                    LoginReminderListener.class.getName());

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(
            ServletContextEvent event) {

        // 明示的に有効化された環境だけで実行する
        if (!"true".equalsIgnoreCase(
                System.getenv("LOGIN_REMINDER_ENABLED"))) {

            LOGGER.info(
                    "ログイン期限通知の定期処理は無効です。");
            return;
        }

        scheduler =
                Executors.newSingleThreadScheduledExecutor(
                        task -> {
                            Thread thread = new Thread(
                                    task,
                                    "anpilink-login-reminder");

                            thread.setDaemon(true);
                            return thread;
                        });

        // 起動から60秒後に開始し、処理終了後1時間ごとに確認する
        scheduler.scheduleWithFixedDelay(
                this::sendReminders,
                60,
                60 * 60,
                TimeUnit.SECONDS);

        LOGGER.info(
                "ログイン期限通知の定期処理を開始しました。");
    }

    /**
     * 通知対象を確認して送信する。
     */
    private void sendReminders() {

        try {
            new LoginReminderService()
                    .sendDueReminders();

        } catch (Exception e) {

            // トークンや通知先の情報はログに出力しない
            LOGGER.log(
                    Level.WARNING,
                    "ログイン期限通知の定期処理失敗: 例外={0}",
                    e.getClass().getSimpleName());
        }
    }

    @Override
    public void contextDestroyed(
            ServletContextEvent event) {

        // 停止・再配置時にバックグラウンド処理を終了する
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}