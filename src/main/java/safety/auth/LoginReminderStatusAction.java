package safety.auth;

import dao.WebPushSubscriptionDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.LoginTokenUtil;

/**
 * この端末がログイン期限通知を受信できるか確認する。
 */
public class LoginReminderStatusAction extends Action {

    @Override
    public void execute(
            HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ビジネスロジック 4
        res.setHeader("Cache-Control", "no-store");

        // GET以外は許可しない
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");
            res.sendError(
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        // セッションではなく、この端末のログイン維持用Cookieを確認する
        String token = LoginTokenUtil.readToken(req);
        if (token == null) {
            res.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 通知を受信したブラウザーの登録情報を取得する
        String endpoint =
                req.getHeader("X-Push-Endpoint");

        if (endpoint == null
                || endpoint.isBlank()
                || endpoint.length() > 2048
                || !endpoint.startsWith("https://")) {

            res.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // DBからデータ取得 3
        WebPushSubscriptionDao dao =
                new WebPushSubscriptionDao();

        boolean allowed = dao.canReceiveLoginReminder(
                LoginTokenUtil.hashToken(token),
                endpoint);

        // レスポンス値をセット 6
        res.setStatus(
                allowed
                        ? HttpServletResponse.SC_NO_CONTENT
                        : HttpServletResponse.SC_FORBIDDEN);
    }
}