package tool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dao.DAO;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * 指定した登録IDへ、Tomcat起動時に1回だけテスト通知する。
 * テスト後は環境変数を削除する。
 */
@WebListener
public class FcmTestListener implements ServletContextListener {

    private ExecutorService executor;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        String value = System.getenv(
                "FCM_TEST_SUBSCRIPTION_ID");

        // 明示的に指定された環境だけで実行する。
        if (value == null || value.isBlank()) {
            return;
        }

        ServletContext context = event.getServletContext();
        long subscriptionId;

        try {
            subscriptionId = Long.parseLong(value.strip());

            if (subscriptionId <= 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {
            context.log(
                    "FCMテスト: 登録IDの設定が正しくありません。");
            return;
        }

        executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(
                    task,
                    "anpilink-fcm-test");

            thread.setDaemon(true);
            return thread;
        });

        executor.submit(() -> {
            try {
                String fcmToken = new TestDao()
                        .findToken(subscriptionId);

                if (fcmToken == null) {
                    context.log(
                            "FCMテスト: 有効な登録がありません。登録ID="
                                    + subscriptionId);
                    return;
                }

                FcmSender.SendResult result = new FcmSender()
                        .send(
                                fcmToken,
                                "AnpiLink サーバーテスト",
                                "Javaサーバーから送信した通知です。");

                if (result.isAccepted()) {
                    context.log(
                            "FCMテスト受付成功: 登録ID="
                                    + subscriptionId
                                    + ", HTTP="
                                    + result.getStatusCode());

                } else {
                    context.log(
                            "FCMテスト受付失敗: HTTP="
                                    + result.getStatusCode()
                                    + ", code="
                                    + result.getErrorCode());
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

            } catch (Exception e) {
                // トークン、秘密鍵、応答本文は記録しない。
                context.log(
                        "FCMテスト処理失敗: 例外="
                                + e.getClass().getSimpleName());

            } finally {
                executor.shutdown();
            }
        });
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 有効なログインに対応する登録だけ取得する。
     */
    private static final class TestDao extends DAO {

        String findToken(long subscriptionId) throws Exception {
            String sql = """
                    SELECT s.fcm_token
                    FROM public.android_push_subscription s
                    JOIN public.api_login_token t
                      ON t.id = s.api_login_token_id
                    JOIN public.users u
                      ON u.id = t.user_id
                    WHERE s.id = ?
                      AND t.expires_at > CURRENT_TIMESTAMP
                      AND u.active = TRUE
                      AND u.role IN ('STUDENT', 'FAMILY')
                    """;

            try (Connection connection = getConnection();
                    PreparedStatement statement =
                            connection.prepareStatement(sql)) {

                statement.setLong(1, subscriptionId);

                try (ResultSet resultSet =
                        statement.executeQuery()) {

                    return resultSet.next()
                            ? resultSet.getString("fcm_token")
                            : null;
                }
            }
        }
    }
}