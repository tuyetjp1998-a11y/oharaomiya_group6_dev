package safety.debug;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import bean.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;
import tool.LoginReminderService;

/**
 * ローカル環境でログイン期限通知を手動確認する。
 */
public class LoginReminderTestAction extends Action {

    @Override
    public void execute(
            HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        res.setHeader("Cache-Control", "no-store");

        // 明示的に有効化されたローカル環境だけで使用する
        String remoteAddress = req.getRemoteAddr();

        boolean loopback =
                "127.0.0.1".equals(remoteAddress)
                || "::1".equals(remoteAddress)
                || "0:0:0:0:0:0:0:1".equals(remoteAddress);

        if (!loopback
                || !"true".equals(System.getenv(
                        "LOCAL_LOGIN_REMINDER_TEST"))) {

            res.sendError(
                    HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        boolean get = "GET".equalsIgnoreCase(req.getMethod());
        boolean post = "POST".equalsIgnoreCase(req.getMethod());

        if (!get && !post) {
            res.setHeader("Allow", "GET, POST");
            res.sendError(
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        HttpSession session = req.getSession(false);

        Object sessionUser = session == null
                ? null
                : session.getAttribute("user");

        if (!(sessionUser instanceof User)) {
            res.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        User user = (User) sessionUser;

        if (!user.isActive()
                || (!"STUDENT".equals(user.getRole())
                    && !"FAMILY".equals(user.getRole()))) {

            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String expectedToken =
                (String) session.getAttribute("csrfToken");

        if (expectedToken == null) {
            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (post) {
            String csrfToken = req.getParameter("csrfToken");

            if (csrfToken == null
                    || !MessageDigest.isEqual(
                            expectedToken.getBytes(
                                    StandardCharsets.UTF_8),
                            csrfToken.getBytes(
                                    StandardCharsets.UTF_8))) {

                res.sendError(
                        HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // 条件に該当する通知登録への送信を実行する
            new LoginReminderService().sendDueReminders();

            res.setContentType("text/plain;charset=UTF-8");
            res.getWriter().println(
                    "確認処理が終了しました。"
                    + "送信結果はTomcatのログを確認してください。");
            return;
        }

        // GETでは確認フォームの表示だけを行う
        res.setContentType("text/html;charset=UTF-8");

        String escapedToken = expectedToken
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        res.getWriter().println("""
                <!DOCTYPE html>
                <html lang="ja">
                <head>
                    <meta charset="UTF-8">
                    <title>ログイン期限通知の動作確認</title>
                </head>
                <body>
                    <h1>ログイン期限通知の動作確認</h1>
                    <p>ローカルDBの条件に該当する端末へ送信します。</p>
                    <form method="post">
                        <input type="hidden"
                               name="csrfToken"
                               value="%s">
                        <button type="submit">確認送信を実行する</button>
                    </form>
                </body>
                </html>
                """.formatted(escapedToken));
    }
}