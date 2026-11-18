package safety.auth;

import java.time.LocalDate;
import java.time.ZoneId;

import bean.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * 生徒・保護者向けの通知設定案内を表示する。
 */
public class NotificationIntroAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        res.setHeader("Cache-Control", "no-store");

        boolean isGet = "GET".equalsIgnoreCase(req.getMethod());
        boolean isPost = "POST".equalsIgnoreCase(req.getMethod());

        if (!isGet && !isPost) {
            res.setHeader("Allow", "GET, POST");
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        HttpSession session = req.getSession(false);
        User user = session == null
                ? null
                : (User) session.getAttribute("user");

        String contextPath = req.getContextPath();

        if (user == null) {
            res.sendRedirect(contextPath + "/safety/auth/Login.action");
            return;
        }

        boolean student = "STUDENT".equals(user.getRole());
        boolean family = "FAMILY".equals(user.getRole());

        if (!user.isActive() || (!student && !family)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String rolePath = student ? "/safety/student" : "/safety/family";
        String homePath = rolePath + "/Home.action";

        if (isPost) {
            String expectedToken = (String) session.getAttribute("csrfToken");

            if (expectedToken == null
                    || !expectedToken.equals(req.getParameter("csrfToken"))) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // 「あとで設定する」を押したらホームへ戻る
            rememberIntroToday(req, res, user);
            res.setStatus(HttpServletResponse.SC_SEE_OTHER);
            res.setHeader("Location", contextPath + homePath);
            return;
        }

        req.setAttribute("homePath", homePath);
        req.setAttribute("subscriptionPath",
                rolePath + "/PushSubscription.action");

        String vapidPublicKey = System.getenv("VAPID_PUBLIC_KEY");
        req.setAttribute("vapidPublicKey",
                vapidPublicKey == null ? "" : vapidPublicKey.strip());

        req.setAttribute("setupId", student
                ? "student-notification-setup"
                : "family-login-reminder-setup");

        req.setAttribute("enableId", student
                ? "student-notification-enable"
                : "family-login-reminder-enable");

        req.setAttribute("messageId", student
                ? "student-notification-message"
                : "family-login-reminder-message");

        req.setAttribute("pushScriptPath", student
                ? "/js/student-push.js"
                : "/js/family-login-reminder.js");

        // 案内を表示した日を記録する
        rememberIntroToday(req, res, user);
        req.getRequestDispatcher("/WEB-INF/views/auth/notification_intro.jsp")
                .forward(req, res);
    }

    private static String introCookieName(User user) {
        return "anpilink_notification_later_" + user.getId();
    }

    private static String todayInJapan() {
        return LocalDate.now(ZoneId.of("Asia/Tokyo")).toString();
    }

    private static void rememberIntroToday(
            HttpServletRequest req, HttpServletResponse res, User user) {

        Cookie cookie = new Cookie(introCookieName(user), todayInJapan());
        String contextPath = req.getContextPath();

        cookie.setPath(contextPath.isEmpty() ? "/" : contextPath);
        cookie.setMaxAge(24 * 60 * 60);
        cookie.setHttpOnly(true);
        cookie.setSecure(req.isSecure());
        cookie.setAttribute("SameSite", "Lax");
        res.addCookie(cookie);
    }

    public static boolean wasShownToday(HttpServletRequest req, User user) {
        Cookie[] cookies = req.getCookies();

        if (cookies == null) {
            return false;
        }

        String name = introCookieName(user);
        String today = todayInJapan();

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())
                    && today.equals(cookie.getValue())) {
                return true;
            }
        }

        return false;
    }
}