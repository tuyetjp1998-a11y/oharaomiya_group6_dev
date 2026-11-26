package safety.principal;

import bean.User;
import dao.PrincipalGroupDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

public class GroupsAction extends Action {
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
        req.setAttribute("groups", new PrincipalGroupDao().findGroups());
        req.getRequestDispatcher("/WEB-INF/views/principal/groups.jsp")
                .forward(req, res);
    }
}
