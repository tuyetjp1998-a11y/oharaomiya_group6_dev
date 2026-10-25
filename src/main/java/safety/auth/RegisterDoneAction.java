package safety.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * 保護者の登録完了画面を表示する。
 */
public class RegisterDoneAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);

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

        if (session == null
                || !Boolean.TRUE.equals(
                        session.getAttribute(
                                "registerCompleted"))) {

            res.sendRedirect(req.getContextPath()
                    + "/safety/auth/Register.action");
            return;
        }

        session.removeAttribute("registerCompleted");

        // DBへデータ保存 5
        // なし

        // レスポンス値をセット 6
        // なし

        // JSPへフォワード 7
        req.getRequestDispatcher(
                "/WEB-INF/views/auth/register_done.jsp")
                .forward(req, res);
    }
}