package safety.student;

import bean.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * 通信状態画面の表示とサーバーへの接続確認を行う。
 */
public class ConnectionAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        User user = session == null
                ? null
                : (User) session.getAttribute("user");

        // リクエストパラメーターの取得 2
        boolean connectionCheck =
                "true".equals(req.getParameter("check"));

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

        if (user == null) {
            if (connectionCheck) {
                // 接続確認ではログイン画面へ転送しない
                res.setStatus(
                        HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                res.sendRedirect(req.getContextPath()
                        + "/safety/auth/Login.action");
            }
            return;
        }

        if (!user.isActive()
                || !"STUDENT".equals(user.getRole())) {

            if (connectionCheck) {
                res.setStatus(
                        HttpServletResponse.SC_FORBIDDEN);
            } else {
                res.sendError(
                        HttpServletResponse.SC_FORBIDDEN);
            }
            return;
        }

        // DBへデータ保存 5
        // なし

        // レスポンス値をセット 6
        if (connectionCheck) {
            // 接続確認に成功した場合は本文なしで返す
            res.setStatus(
                    HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        // JSPへフォワード 7
        req.getRequestDispatcher(
                "/WEB-INF/views/student/connection.jsp")
                .forward(req, res);
    }
}