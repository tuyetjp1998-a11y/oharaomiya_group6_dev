package safety.principal;

import bean.User;
import dao.PrincipalGroupDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

public class GroupAssignSubmitAction extends Action {
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
        if (!user.isActive() || !"PRINCIPAL".equals(user.getRole())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "POST");
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        String token = (String) session.getAttribute("csrfToken");
        if (token == null || !token.equals(req.getParameter("csrfToken"))) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        PrincipalGroupDao dao = new PrincipalGroupDao();
        long classId = 0;
        Long teacherId = null;
        String error;
        try {
            classId = Long.parseLong(req.getParameter("classId"));
            String value = req.getParameter("teacherId");
            if (value == null) throw new IllegalArgumentException("職員を確認してください。");
            if (!value.isEmpty()) teacherId = Long.valueOf(value);
            dao.assignTeacher(classId, teacherId);
            session.setAttribute("groupAssignDone", Boolean.TRUE);
            res.setStatus(HttpServletResponse.SC_SEE_OTHER);
            res.setHeader("Location", req.getContextPath() + "/safety/principal/GroupAssignDone.action");
            return;
        } catch (IllegalArgumentException e) {
            error = "クラスまたは職員を確認してください。";
        } catch (java.sql.SQLException | javax.naming.NamingException e) {
            req.getServletContext().log("職員の割り当てに失敗しました。", e);
            error = "職員の割り当てに失敗しました。管理者にお問い合わせください。";
        }
        req.setAttribute("assignError", error);
        req.setAttribute("groups", dao.findGroups());
        req.setAttribute("teachers", dao.findTeachers());
        req.setAttribute("selectedClass", classId);
        req.setAttribute("selectedTeacher", teacherId);
        req.getRequestDispatcher("/WEB-INF/views/principal/group_assign.jsp").forward(req, res);
    }
}
