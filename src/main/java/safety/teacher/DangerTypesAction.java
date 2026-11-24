package safety.teacher;

import java.time.LocalDate;
import java.time.ZoneId;

import bean.User;
import dao.TeacherClassDao;
import dao.WorriedTypeDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/** 「困っている」の登録済み項目を表示する。 */
public class DangerTypesAction extends Action {

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

        int schoolYear = LocalDate.now(ZoneId.of("Asia/Tokyo"))
                .minusMonths(3).getYear();
        req.setAttribute(
                "worriedTypes",
                new WorriedTypeDao().findActive());
        req.setAttribute(
                "assignedClasses",
                new TeacherClassDao().findAssignedClasses(
                        user.getId(), schoolYear));
        req.setAttribute("showDangerTypes", true);
        req.setAttribute(
                "dangerTypeMessage",
                session.getAttribute("dangerTypeMessage"));
        session.removeAttribute("dangerTypeMessage");

        req.getRequestDispatcher(
                "/WEB-INF/views/teacher/danger_types.jsp")
                .forward(req, res);
    }
}
