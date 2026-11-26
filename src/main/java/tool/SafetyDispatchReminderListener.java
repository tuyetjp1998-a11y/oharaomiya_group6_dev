package tool;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Tomcat起動時に催促通知の定期処理を開始する。 */
@WebListener
public final class SafetyDispatchReminderListener implements ServletContextListener {
    private ScheduledExecutorService executor;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "anpilink-dispatch-reminder");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleWithFixedDelay(new SafetyDispatchReminderService(), 60, 60, TimeUnit.SECONDS);
        event.getServletContext().log("安否確認催促通知の定期処理を開始しました。");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (executor != null) {
            executor.shutdownNow();
            try { executor.awaitTermination(10, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
}
