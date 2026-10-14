package safety.student;

import bean.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * 通信状態のデモ画面を表示するAction。
 */
public class ConnectionAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        User user = null;
        boolean onlineDemo;

        // リクエストパラメーターの取得 2
        String view = req.getParameter("view");

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

        if (session != null) {
            user = (User) session.getAttribute("user");
        }

        if (user == null) {
            res.sendRedirect(req.getContextPath()
                    + "/safety/auth/Login.action");
            return;
        }

        if (!user.isActive()
                || !"STUDENT".equals(user.getRole())) {
            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (view != null
                && !"offline".equals(view)
                && !"online".equals(view)) {
            res.sendError(
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        onlineDemo = "online".equals(view);

        // DBへデータ保存 5
        // なし（デモ画面では回答を送信しない）

        // レスポンス値をセット 6
        req.setAttribute("onlineDemo", onlineDemo);

        // JSPへフォワード 7
        req.getRequestDispatcher(
                "/WEB-INF/views/student/connection.jsp")
                .forward(req, res);
    }
}