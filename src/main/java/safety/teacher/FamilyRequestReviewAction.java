package safety.teacher;

import java.time.LocalDate;
import java.time.ZoneId;

import bean.User;
import dao.FamilyRequestReviewDao;
import dao.TeacherClassDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/** 親子関係申請を承認または却下する。 */
public class FamilyRequestReviewAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        req.setCharacterEncoding("UTF-8");
        res.setHeader("Cache-Control", "no-store");

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

        int schoolYear = LocalDate.now(ZoneId.of("Asia/Tokyo"))
                .minusMonths(3).getYear();

        if ("GET".equalsIgnoreCase(req.getMethod())) {
            showCompleted(req, res, session, user, schoolYear);
            return;
        }

        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET, POST");
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        String expectedToken =
                (String) session.getAttribute("csrfToken");
        String csrfToken = req.getParameter("csrfToken");
        if (expectedToken == null
                || !expectedToken.equals(csrfToken)) {
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

        String decision = req.getParameter("decision");
        String rejectionReason = req.getParameter("rejectionReason");
        rejectionReason = rejectionReason == null
                ? ""
                : rejectionReason.strip();

        if ("APPROVED".equals(decision)) {
            if (!"confirmed".equals(
                    req.getParameter("relationshipConfirmed"))) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            rejectionReason = null;
        } else if ("REJECTED".equals(decision)) {
            if (rejectionReason.isEmpty()
                    || rejectionReason.length() > 300) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        } else {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        boolean reviewed = new FamilyRequestReviewDao().review(
                requestId,
                user.getId(),
                schoolYear,
                decision,
                rejectionReason);

        if (!reviewed) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        session.setAttribute("familyReviewCompleted", decision);
        res.setStatus(HttpServletResponse.SC_SEE_OTHER);
        res.setHeader(
                "Location",
                req.getContextPath()
                        + "/safety/teacher/FamilyRequestReview.action"
                        + "?result="
                        + ("APPROVED".equals(decision)
                                ? "approved"
                                : "rejected"));
    }

    private void showCompleted(
            HttpServletRequest req,
            HttpServletResponse res,
            HttpSession session,
            User user,
            int schoolYear) throws Exception {

        String result = req.getParameter("result");
        String expected = "approved".equals(result)
                ? "APPROVED"
                : "rejected".equals(result)
                        ? "REJECTED"
                        : null;
        Object completed = session.getAttribute(
                "familyReviewCompleted");

        if (expected == null || !expected.equals(completed)) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        session.removeAttribute("familyReviewCompleted");

        req.setAttribute("showFamilyRequests", true);
        req.setAttribute(
                "assignedClasses",
                new TeacherClassDao().findAssignedClasses(
                        user.getId(), schoolYear));

        String page = "APPROVED".equals(expected)
                ? "family_request_approved.jsp"
                : "family_request_rejected.jsp";
        req.getRequestDispatcher(
                "/WEB-INF/views/teacher/" + page)
                .forward(req, res);
    }
}
