package safety.teacher;

import java.time.LocalDate;
import java.time.ZoneId;

import bean.FamilyRequestReview;
import bean.User;
import dao.FamilyRequestReviewDao;
import dao.TeacherClassDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/** 担当クラスの親子関係申請を確認する。 */
public class FamilyRequestDetailAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        res.setHeader("Cache-Control", "no-store");

        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        HttpSession session = req.getSession(false);
        User user = session == null
                ? null
                : (User) session.getAttribute("user");

        if (user == null) {
            res.sendRedirect(req.getContextPath()
                    + "/safety/auth/Login.action");
            return;
        }
        if (!user.isActive() || !"TEACHER".equals(user.getRole())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        long requestId;
        try {
            requestId = Long.parseLong(req.getParameter("requestId"));
        } catch (NumberFormatException e) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        int schoolYear = LocalDate.now(ZoneId.of("Asia/Tokyo"))
                .minusMonths(3).getYear();
        FamilyRequestReview request =
                new FamilyRequestReviewDao().findPendingById(
                        requestId, user.getId(), schoolYear);

        if (request == null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String mode = req.getParameter("mode");
        boolean rejectMode = "reject".equals(mode);
        req.setAttribute("familyRequest", request);
        req.setAttribute("showFamilyRequests", true);
        req.setAttribute(
                "assignedClasses",
                new TeacherClassDao().findAssignedClasses(
                        user.getId(), schoolYear));

        String page = rejectMode
                ? "family_request_reject.jsp"
                : "family_request_detail.jsp";
        req.getRequestDispatcher(
                "/WEB-INF/views/teacher/" + page)
                .forward(req, res);
    }
}
