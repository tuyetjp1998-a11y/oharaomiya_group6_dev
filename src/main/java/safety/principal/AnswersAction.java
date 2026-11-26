package safety.principal;

import bean.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

public class AnswersAction extends Action {

    @Override
    public void execute(
            HttpServletRequest req,
            HttpServletResponse res)
            throws Exception {

        // 1. セッション取得
        HttpSession session = req.getSession(false);

        User user = session == null
                ? null
                : (User) session.getAttribute("user");

        // キャッシュ禁止
        res.setHeader("Cache-Control", "no-store");

        // GET以外は禁止
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");
            res.sendError(
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        // 未ログイン
        if (user == null) {
            res.sendRedirect(
                    req.getContextPath()
                    + "/safety/auth/Login.action");
            return;
        }

        // 校長以外は禁止
        if (!user.isActive()
                || !"PRINCIPAL".equals(user.getRole())) {

            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // 2. パラメータ取得
        String grade = req.getParameter("grade");
        String classId = req.getParameter("class");
        String studentId = req.getParameter("student");

        // 3. JSPへ渡す
        req.setAttribute("grade", grade);
        req.setAttribute("classId", classId);
        req.setAttribute("studentId", studentId);

        req.setAttribute(
                "pageTitle",
                "全体の安否確認");

        // 4. JSPへフォワード
        req.getRequestDispatcher(
                "/WEB-INF/views/principal/answers.jsp")
                .forward(req, res);
    }
}