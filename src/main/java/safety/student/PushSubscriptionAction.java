package safety.student;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import bean.User;
import dao.WebPushSubscriptionDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * 生徒のWeb Push登録情報を保存する。
 */
public class PushSubscriptionAction extends Action {

    @Override
    public void execute(
            HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        res.setHeader("Cache-Control", "no-store");

        // POST以外は許可しない
        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "POST");
            res.sendError(
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        req.setCharacterEncoding("UTF-8");

        // ログイン状態を確認する
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

        // 有効な生徒アカウントのみ許可する
        if (!user.isActive()
                || !"STUDENT".equals(user.getRole())) {
            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // CSRFトークンを確認する
        String expectedToken =
                (String) session.getAttribute("csrfToken");
        String csrfToken = req.getParameter("csrfToken");

        if (expectedToken == null
                || csrfToken == null
                || !MessageDigest.isEqual(
                        expectedToken.getBytes(
                                StandardCharsets.UTF_8),
                        csrfToken.getBytes(
                                StandardCharsets.UTF_8))) {

            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // この端末のログイン維持用トークンIDを取得する
        Object tokenIdValue =
                session.getAttribute("loginTokenId");

        if (!(tokenIdValue instanceof Long)) {
            res.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        long loginTokenId = (Long) tokenIdValue;

        // 通知登録情報を取得する
        String endpoint = req.getParameter("endpoint");
        String p256dh = req.getParameter("p256dh");
        String auth = req.getParameter("auth");

        if (endpoint == null || endpoint.isBlank()
                || p256dh == null || p256dh.isBlank()
                || auth == null || auth.isBlank()) {

            res.sendError(
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // DBの最大長と送信先の形式を確認する
        if (endpoint.length() > 2048
                || p256dh.length() > 255
                || auth.length() > 255
                || !endpoint.startsWith("https://")) {

            res.sendError(
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        WebPushSubscriptionDao dao =
                new WebPushSubscriptionDao();

        boolean saved = dao.saveStudentSubscription(
                user.getId(),
                loginTokenId,
                endpoint,
                p256dh,
                auth);

        if (!saved) {
            // 期限切れの場合はブラウザー側で再登録する
            if (dao.isExpiredForEndpoint(
                    user.getId(), endpoint)) {

                res.setStatus(
                        HttpServletResponse.SC_GONE);
                return;
            }

            res.setStatus(
                    HttpServletResponse.SC_CONFLICT);
            return;
        }

        // 保存成功
        res.setStatus(
                HttpServletResponse.SC_NO_CONTENT);
    }
}