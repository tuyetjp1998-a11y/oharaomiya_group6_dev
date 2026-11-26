package safety.principal;

import bean.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

public class StaffEditAction extends Action {
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
        long id;
        try { id = Long.parseLong(req.getParameter("teacherId")); if (id <= 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { res.sendError(400); return; }
        var staff = new dao.PrincipalStaffDao().findById(id);
        if (staff == null) { res.sendError(404); return; }
        req.setAttribute("staff", staff);
        req.setAttribute("staffName", staff.getName());
        req.setAttribute("staffActive", "利用中".equals(staff.getStatus()));
        req.getRequestDispatcher("/WEB-INF/views/principal/staff_edit.jsp").forward(req, res);
    }
}
