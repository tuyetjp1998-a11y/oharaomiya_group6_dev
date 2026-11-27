package safety.principal;

import bean.User;
import dao.PrincipalSafetyDao;
import dao.PrincipalSafetyDao.ClassStudentsResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

public class StudentsAction extends Action {

    @Override
    public void execute(
            HttpServletRequest req,
            HttpServletResponse res)
            throws Exception {

        HttpSession session = req.getSession(false);

        User user = session == null
                ? null
                : (User) session.getAttribute("user");

        res.setHeader("Cache-Control", "no-store");

        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        if (user == null) {
            res.sendRedirect(
                    req.getContextPath()
                    + "/safety/auth/Login.action");
            return;
        }

        if (!user.isActive()
                || !"PRINCIPAL".equals(user.getRole())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String classIdParam = req.getParameter("classId");
        String schoolYearParam = req.getParameter("schoolYear");

        long classId;
        int schoolYear;

        try {
            classId = Long.parseLong(classIdParam);
            schoolYear = Integer.parseInt(schoolYearParam);
        } catch (NumberFormatException e) {
            res.sendRedirect(
                    req.getContextPath()
                    + "/safety/principal/Answers.action");
            return;
        }

        PrincipalSafetyDao dao = new PrincipalSafetyDao();

        ClassStudentsResult classData =
                dao.findStudentsByClass(classId, schoolYear);

        if (classData == null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        req.setAttribute("classData", classData);
        req.setAttribute(
                "pageTitle",
                classData.getClassName() + "の安否確認");

        req.getRequestDispatcher(
                "/WEB-INF/views/principal/answer_students.jsp")
                .forward(req, res);
    }
}
