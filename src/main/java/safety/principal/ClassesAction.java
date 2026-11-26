package safety.principal;

import java.util.List;

import bean.User;
import dao.PrincipalSafetyDao;
import dao.PrincipalSafetyDao.ClassSummary;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

public class ClassesAction extends Action {

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

        res.setHeader("Cache-Control", "no-store");

        // GETのみ
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

        // 校長のみ
        if (!user.isActive()
                || !"PRINCIPAL".equals(user.getRole())) {

            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // 2. 学年取得
        String gradeParam = req.getParameter("grade");

        int grade;

        try {
            grade = Integer.parseInt(gradeParam);
        } catch (NumberFormatException e) {
            res.sendRedirect(
                    req.getContextPath()
                    + "/safety/principal/Answers.action");
            return;
        }

        if (grade < 1 || grade > 3) {
            res.sendRedirect(
                    req.getContextPath()
                    + "/safety/principal/Answers.action");
            return;
        }

        // 3. DBからクラス別安否状況を取得
        PrincipalSafetyDao dao =
                new PrincipalSafetyDao();

        List<ClassSummary> classes =
                dao.findClassesByGrade(grade);

        // 4. JSPへ渡す
        req.setAttribute("grade", grade);
        req.setAttribute("classes", classes);
        req.setAttribute(
                "pageTitle",
                grade + "年生の安否確認");

        // 5. JSPへフォワード
        req.getRequestDispatcher(
                "/WEB-INF/views/principal/answer_classes.jsp")
                .forward(req, res);
    }
}
