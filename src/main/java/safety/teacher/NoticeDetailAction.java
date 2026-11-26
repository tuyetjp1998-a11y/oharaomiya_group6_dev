package safety.teacher;

import bean.User;
import dao.TeacherNoticeDao;
import dao.SafetyDispatchDao;
import dao.TeacherClassDao;
import java.time.LocalDate;
import java.time.ZoneId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

public class NoticeDetailAction extends Action {
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        req.setCharacterEncoding("UTF-8");
        res.setHeader("Cache-Control", "no-store");
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/safety/auth/Login.action");
            return;
        }
        if (!user.isActive() || !"TEACHER".equals(user.getRole())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        boolean post = "POST".equalsIgnoreCase(req.getMethod());
        if (!post && !"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET, POST");
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        String[] ids = req.getParameterValues("dispatchId");
        long dispatchId;
        try {
            if (ids == null || ids.length != 1 || !ids[0].matches("[1-9][0-9]{0,18}")) {
                throw new NumberFormatException();
            }
            dispatchId = Long.parseLong(ids[0]);
        } catch (NumberFormatException e) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        var notice = new TeacherNoticeDao().findDetail(user.getId(), dispatchId);
        if (notice == null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (post) {
            String expectedToken = (String) session.getAttribute("csrfToken");
            if (expectedToken == null || !expectedToken.equals(req.getParameter("csrfToken"))) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (!new SafetyDispatchDao().markRead(user.getId(), dispatchId, "TEACHER")) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            res.setStatus(HttpServletResponse.SC_SEE_OTHER);
            res.setHeader("Location", req.getContextPath()
                    + "/safety/teacher/NoticeDetail.action?dispatchId=" + dispatchId);
            return;
        }
        req.setAttribute("notice", notice);
        int schoolYear = LocalDate.now(ZoneId.of("Asia/Tokyo")).minusMonths(3).getYear();
        req.setAttribute("assignedClasses", new TeacherClassDao().findAssignedClasses(user.getId(), schoolYear));
        req.setAttribute("showNotices", true);
        req.getRequestDispatcher("/WEB-INF/views/teacher/staff_notice_detail.jsp").forward(req, res);
    }
}
