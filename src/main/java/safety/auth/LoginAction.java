package safety.auth;

import java.security.SecureRandom;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

public class LoginAction extends Action {

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res)
            throws Exception {

        // ローカル変数の宣言 1
        HttpSession session;
        String csrfToken;
        byte[] randomBytes;
        SecureRandom secureRandom;

        // リクエストパラメーターの取得 2
        // なし

        // DBからデータ取得 3
        // なし

        // ビジネスロジック 4
        // GET以外のリクエストは受け付けない
        if (!"GET".equals(req.getMethod())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        session = req.getSession();
        csrfToken = (String) session.getAttribute("csrfToken");

        // CSRFトークンが存在しない場合は生成する
        if (csrfToken == null) {
            randomBytes = new byte[32];
            secureRandom = new SecureRandom();
            secureRandom.nextBytes(randomBytes);

            csrfToken = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(randomBytes);
        }

        // DBへデータ保存 5
        // なし

        // レスポンス値をセット 6
        session.setAttribute("csrfToken", csrfToken);

        // JSPへフォワード 7
        req.getRequestDispatcher(
                "/WEB-INF/views/auth/login.jsp"
        ).forward(req, res);
    }
}