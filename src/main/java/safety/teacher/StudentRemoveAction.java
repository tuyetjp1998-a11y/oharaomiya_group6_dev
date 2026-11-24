package safety.teacher;

import java.time.LocalDate;
import java.time.ZoneId;

import bean.User;
import dao.TeacherClassDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/** グループ画面で選択した生徒のクラス所属を解除する。 */
public class StudentRemoveAction extends Action {
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        res.setHeader("Cache-Control", "no-store");
        if (!"POST".equals(req.getMethod())) {
            res.setHeader("Allow", "POST");
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/safety/auth/Login.action");
            return;
        }
        String token = (String) session.getAttribute("csrfToken");
        if (!user.isActive() || !"TEACHER".equals(user.getRole())
                || token == null || !token.equals(req.getParameter("csrfToken"))) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        long classId;
        long studentId;
        try {
            classId = Long.parseLong(req.getParameter("classId"));
            studentId = Long.parseLong(req.getParameter("studentId"));
        } catch (NumberFormatException ex) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        int year = LocalDate.now(ZoneId.of("Asia/Tokyo")).minusMonths(3).getYear();
        boolean removed = new TeacherClassDao().removeStudent(user.getId(), year, classId, studentId);
        String path = "/safety/teacher/Home.action?view=groups";
        if (removed) {
            session.setAttribute("studentRemoved", Boolean.TRUE);
            path += "&done=removed";
        }
        // 既に所属がない場合も最新の一覧へ戻す。
        res.setStatus(HttpServletResponse.SC_SEE_OTHER);
        res.setHeader("Location", req.getContextPath() + path);
    }
}
