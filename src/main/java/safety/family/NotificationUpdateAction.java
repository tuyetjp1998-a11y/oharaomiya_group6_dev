
package safety.family;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import bean.User;
import dao.UserDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * 保護者の通知設定を更新する。
 */
public class NotificationUpdateAction extends Action {

    @Override
    public void execute(
            HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        UserDao userDao = new UserDao();

        User user = session == null
                ? null
                : (User) session.getAttribute("user");

        // リクエストパラメーターの取得 2
        String enabled = req.getParameter("enabled");
        String csrfToken = req.getParameter("csrfToken");

        // ビジネスロジック 4
        res.setHeader("Cache-Control", "no-store");

        // POST以外は拒否
        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "POST");
            res.sendError(
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        // ログイン確認
        if (user == null) {
            res.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 保護者権限の確認
        if (!user.isActive()
                || !"FAMILY".equals(user.getRole())) {
            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // CSRFトークン確認
        String expectedToken = session == null
                ? null
                : (String) session.getAttribute("csrfToken");

        if (expectedToken == null
                || csrfToken == null
                || !MessageDigest.isEqual(
                        expectedToken.getBytes(
                                StandardCharsets.UTF_8),
                        csrfToken.getBytes(
                                StandardCharsets.UTF_8))) {

            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "不正なリクエストです。");
            return;
        }

        // 入力値確認
        // ONへの変更はPushSubscriptionActionで行う
        if (!"false".equals(enabled)) {
            res.sendError(
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // DBへデータ保存 5
        boolean updated =
                userDao.updateNotificationEnabled(
                        user.getId(),
                        false);

        if (!updated) {
            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // レスポンス値をセット 6
        res.setStatus(
                HttpServletResponse.SC_NO_CONTENT);
    }
}