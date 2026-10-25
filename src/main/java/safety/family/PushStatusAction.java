package safety.family;

import bean.User;
import dao.WebPushSubscriptionDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * 現在の利用者が通知登録の所有者か確認する。
 */
public class PushStatusAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        User user = null;

        // リクエストパラメーターの取得 2
        // endpointをURLやアクセスログに含めない
        String endpoint = req.getHeader("X-Push-Endpoint");

        // DBからデータ取得 3
        // 認証確認後に取得する

        // ビジネスロジック 4
        res.setHeader("Cache-Control", "no-store");

        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");
            res.setStatus(
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        if (session != null
                && session.getAttribute("user") instanceof User) {
            user = (User) session.getAttribute("user");
        }

        if (user == null) {
            res.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (!user.isActive()
                || !"FAMILY".equals(user.getRole())) {
            res.setStatus(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (endpoint == null
                || endpoint.isBlank()
                || endpoint.length() > 2048) {
            res.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        boolean allowed = new WebPushSubscriptionDao()
                .isEnabledForEndpoint(user.getId(), endpoint);

        // DBへデータ保存 5
        // なし

        // レスポンス値をセット 6
        res.setStatus(allowed
                ? HttpServletResponse.SC_NO_CONTENT
                : HttpServletResponse.SC_FORBIDDEN);

        // JSPへフォワード 7
        // なし
    }
}