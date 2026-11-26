package safety.principal;

import bean.User;
import dao.PrincipalSafetyDao;
import dao.PrincipalSafetyDao.HomeStudent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/** 校長用の生徒の最新安否回答詳細。 */
public class AnswerDetailAction extends Action {

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res)
            throws Exception {
        res.setHeader("Cache-Control", "no-store");
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/safety/auth/Login.action");
            return;
        }
        if (!user.isActive() || !"PRINCIPAL".equals(user.getRole())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        long studentId;
        Long classId = null;
        Integer schoolYear = null;
        try {
            studentId = Long.parseLong(req.getParameter("studentId"));
            String classParam = req.getParameter("classId");
            String yearParam = req.getParameter("schoolYear");
            if (classParam != null || yearParam != null) {
                classId = Long.parseLong(classParam);
                schoolYear = Integer.parseInt(yearParam);
            }
            if (studentId <= 0 || (classId != null && classId <= 0)
                    || (schoolYear != null && (schoolYear < 2000 || schoolYear > 9999))) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        } catch (NumberFormatException e) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        HomeStudent student = new PrincipalSafetyDao().findStudentDetail(studentId);
        if (student == null || (classId != null &&
                (!classId.equals(student.getClassId())
                || !schoolYear.equals(student.getSchoolYear())))) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        req.setAttribute("student", student);
        req.setAttribute("fromHome", "home".equals(req.getParameter("from")));
        req.setAttribute("pageTitle", "回答詳細");
        req.getRequestDispatcher("/WEB-INF/views/principal/answer_detail.jsp")
                .forward(req, res);
    }
}
