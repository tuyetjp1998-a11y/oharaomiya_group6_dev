package safety.teacher;

import bean.User;
import dao.TeacherNoticeDao;
import dao.TeacherClassDao;
import java.time.LocalDate;
import java.time.ZoneId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

public class NoticesAction extends Action {
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
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        req.setAttribute("notices", new TeacherNoticeDao().findByTeacher(user.getId()));
        int schoolYear = LocalDate.now(ZoneId.of("Asia/Tokyo")).minusMonths(3).getYear();
        req.setAttribute("assignedClasses", new TeacherClassDao().findAssignedClasses(user.getId(), schoolYear));
        req.setAttribute("showNotices", true);
        req.getRequestDispatcher("/WEB-INF/views/teacher/staff_notices.jsp").forward(req, res);
    }
}
