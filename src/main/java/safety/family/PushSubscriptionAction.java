package safety.family;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import bean.User;
import dao.WebPushSubscriptionDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * 保護者のWeb Push登録情報を保存する。
 */
public class PushSubscriptionAction extends Action {

    @Override
    public void execute(
            HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // レスポンスをキャッシュさせない
        res.setHeader("Cache-Control", "no-store");

        // POST以外は許可しない
        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "POST");
            res.sendError(
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        req.setCharacterEncoding("UTF-8");

        // セッションを取得する
        HttpSession session = req.getSession(false);

        if (session == null) {
            res.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // ログインユーザーを確認する
        Object sessionUser = session.getAttribute("user");

        if (!(sessionUser instanceof User)) {
            res.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        User user = (User) sessionUser;

        // 有効な保護者のみ許可する
        if (!"FAMILY".equals(user.getRole())
                || !user.isActive()) {

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

        // この端末のログイン維持用トークンIDを確認する
        Object tokenIdValue =
                session.getAttribute("loginTokenId");

        if (!(tokenIdValue instanceof Long)) {
            res.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        long loginTokenId = (Long) tokenIdValue;

        // 登録情報と登録目的を取得する
        String endpoint = req.getParameter("endpoint");
        String p256dh = req.getParameter("p256dh");
        String auth = req.getParameter("auth");
        String purpose = req.getParameter("purpose");

        // 必須項目を確認する
        if (endpoint == null || endpoint.isBlank()
                || p256dh == null || p256dh.isBlank()
                || auth == null || auth.isBlank()) {

            res.sendError(
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // 入力サイズとエンドポイント形式を確認する
        if (endpoint.length() > 2048
                || p256dh.length() > 255
                || auth.length() > 255
                || !endpoint.startsWith("https://")) {

            res.sendError(
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // 未指定の場合は従来の安否通知ON操作とする
        if (purpose != null
                && !"LOGIN_REMINDER".equals(purpose)) {

            res.sendError(
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        boolean reminderOnly =
                "LOGIN_REMINDER".equals(purpose);

        WebPushSubscriptionDao dao =
                new WebPushSubscriptionDao();

        boolean saved;

        if (reminderOnly) {

            // 安否通知のON／OFFを変更せずに保存する
            saved = dao.saveFamilyReminderSubscription(
                    user.getId(),
                    loginTokenId,
                    endpoint,
                    p256dh,
                    auth);

        } else {

            // 従来の安否通知ON操作
            saved = dao.enable(
                    user.getId(),
                    endpoint,
                    p256dh,
                    auth);
        }

        if (!saved) {

            // 自分の期限切れ登録の場合は再登録を求める
            if (dao.isExpiredForEndpoint(
                    user.getId(), endpoint)) {

                res.setStatus(
                        HttpServletResponse.SC_GONE);
                return;
            }

            // 他の利用者が所有する登録などは上書きしない
            res.setStatus(
                    HttpServletResponse.SC_CONFLICT);
            return;
        }

        // 期限通知用の登録ではDAO内で関連付け済み
        if (!reminderOnly) {

            boolean linked = dao.bindLoginToken(
                    user.getId(),
                    endpoint,
                    loginTokenId);

            if (!linked) {
                res.sendError(
                        HttpServletResponse.SC_CONFLICT);
                return;
            }
        }

        // 保存成功
        res.setStatus(
                HttpServletResponse.SC_NO_CONTENT);
    }
}