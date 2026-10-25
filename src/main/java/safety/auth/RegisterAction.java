package safety.auth;

import java.security.SecureRandom;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * 保護者の新規登録画面を表示する。
 */
public class RegisterAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session;
        String csrfToken;

        // リクエストパラメーターの取得 2
        // なし

        // DBからデータ取得 3
        // なし

        // ビジネスロジック 4
        res.setHeader("Cache-Control", "no-store");

        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");
            res.sendError(
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        session = req.getSession();
        csrfToken = (String) session.getAttribute("csrfToken");

        if (csrfToken == null) {
            byte[] tokenBytes = new byte[32];
            new SecureRandom().nextBytes(tokenBytes);

            csrfToken = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(tokenBytes);
        }

        // DBへデータ保存 5
        // なし

        // レスポンス値をセット 6
        session.setAttribute("csrfToken", csrfToken);

        // JSPへフォワード 7
        req.getRequestDispatcher(
                "/WEB-INF/views/auth/register.jsp")
                .forward(req, res);
    }
}