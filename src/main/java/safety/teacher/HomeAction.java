package safety.teacher;

import bean.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

public class HomeAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        User user = session == null
                ? null
                : (User) session.getAttribute("user");

        // リクエストパラメーターの取得 2
        // なし

        // DBからデータ取得 3
        // なし
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        
        // ビジネスロジック 4
        if (user == null) {
            res.sendRedirect(req.getContextPath()
                    + "/safety/auth/Login.action");
            return;
        }

        if (!user.isActive()
                || !"TEACHER".equals(user.getRole())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // DBへデータ保存 5
        // なし

        // レスポンス値をセット 6
        res.setHeader("Cache-Control", "no-store");
        req.setAttribute("pageTitle", "教員トップ");

        // JSPへフォワード 7
        req.getRequestDispatcher("/WEB-INF/views/teacher/home.jsp")
                .forward(req, res);
    }
}