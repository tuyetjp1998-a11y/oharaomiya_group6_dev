package safety.auth;

import java.security.SecureRandom;
import java.util.Base64;

import bean.User;
import dao.UserDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;
import tool.PasswordUtil;

public class LoginExecuteAction extends Action {

    @Override
    public void execute(
            HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        UserDao userDao = new UserDao();
        UserDao.LoginData loginData = null;
        User user = null;

        boolean loginFailed = true;
        String redirectPath = null;

        // リクエストパラメーターの取得 2
        String loginId = req.getParameter("loginId");
        String password = req.getParameter("password");
        String role = req.getParameter("role");
        String csrfToken = req.getParameter("csrfToken");

        // POST以外のリクエストを拒否
        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "POST");
            res.sendError(
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        // CSRFトークンを確認
        String expectedToken = session == null
                ? null
                : (String) session.getAttribute("csrfToken");

        if (expectedToken == null
                || csrfToken == null
                || !expectedToken.equals(csrfToken)) {

            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "ログイン画面を開き直してください。");
            return;
        }

        // DBからデータ取得 3
        if (loginId != null
                && !loginId.isBlank()
                && loginId.length() <= 100
                && password != null
                && !password.isEmpty()
                && role != null) {

            loginData = userDao.findByLoginId(loginId);
        }

        // ビジネスロジック 4
        if (loginData != null) {

            user = loginData.getUser();

            boolean passwordCorrect = PasswordUtil.verify(
                    password,
                    loginData.getPasswordHash());

            if (passwordCorrect
                    && user.isActive()
                    && role.equals(user.getRole())) {

                // 遷移先はDBに保存された利用者区分で決定
                switch (user.getRole()) {
                    case "STUDENT":
                        redirectPath =
                                "/safety/student/Home.action";
                        break;

                    case "FAMILY":
                        redirectPath =
                                "/safety/family/Home.action";
                        break;

                    case "TEACHER":
                        redirectPath =
                                "/safety/teacher/Home.action";
                        break;

                    case "PRINCIPAL":
                        redirectPath =
                                "/safety/principal/Home.action";
                        break;

                    default:
                        redirectPath = null;
                        break;
                }

                loginFailed = redirectPath == null;
            }
        }

        // DBへデータ保存 5
        // なし（ログインではDBの更新を行わない）

        // レスポンス値をセット 6
        res.setHeader("Cache-Control", "no-store");

        if (loginFailed) {

            req.setAttribute("loginFailed", true);

        } else {

            // セッション固定攻撃対策
            req.changeSessionId();

            // ログイン済み利用者を保存
            session.setAttribute("user", user);

            // ログイン後のCSRFトークンを再生成
            byte[] tokenBytes = new byte[32];
            new SecureRandom().nextBytes(tokenBytes);

            String newCsrfToken = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(tokenBytes);

            session.setAttribute("csrfToken", newCsrfToken);

            // 無操作時のセッション有効期間：30分
            session.setMaxInactiveInterval(30 * 60);
        }

        // JSPへフォワード 7
        if (loginFailed) {

            req.getRequestDispatcher(
                    "/WEB-INF/views/auth/login.jsp")
                    .forward(req, res);

        } else {

            // 成功時はリダイレクトしてフォーム再送信を防ぐ
            res.sendRedirect(
                    req.getContextPath() + redirectPath);
        }
    }
}