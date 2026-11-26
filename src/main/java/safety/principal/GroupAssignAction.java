package safety.principal;

import bean.User;
import dao.PrincipalGroupDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

public class GroupAssignAction extends Action {
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
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        PrincipalGroupDao dao = new PrincipalGroupDao();
        var groups = dao.findGroups();
        long classId = groups.isEmpty() ? 0 : groups.get(0).getClassId();
        String requested = req.getParameter("classId");
        if (requested != null) {
            try { classId = Long.parseLong(requested); }
            catch (NumberFormatException e) { res.sendError(400); return; }
            final long selected = classId;
            if (groups.stream().noneMatch(g -> g.getClassId() == selected)) { res.sendError(404); return; }
        }
        req.setAttribute("groups", groups);
        req.setAttribute("teachers", dao.findTeachers());
        req.setAttribute("selectedClass", classId);
        req.setAttribute("selectedTeacher", classId == 0 ? null : dao.findAssignedTeacher(classId));
        req.getRequestDispatcher("/WEB-INF/views/principal/group_assign.jsp").forward(req, res);
    }
}
