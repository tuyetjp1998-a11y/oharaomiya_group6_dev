package safety.principal;

import bean.User;
import dao.WorriedTypeDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/** 「困っている」の項目を選択肢から外す。 */
public class DangerTypeDeleteAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        req.setCharacterEncoding("UTF-8");
        res.setHeader("Cache-Control", "no-store");
        if (!"POST".equalsIgnoreCase(req.getMethod()) && !"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET, POST");
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
        if (!user.isActive() || !"PRINCIPAL".equals(user.getRole())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if ("GET".equalsIgnoreCase(req.getMethod())) {
            Object name = session.getAttribute("principalDangerTypeDeleted");
            if (name == null) {
                res.sendRedirect(req.getContextPath() + "/safety/principal/DangerTypes.action");
                return;
            }
            req.setAttribute("worriedTypes", new WorriedTypeDao().findActive());
            session.removeAttribute("principalDangerTypeDeleted");
            req.setAttribute("deletedName", name);
            req.getRequestDispatcher("/WEB-INF/views/principal/danger_type_deleted.jsp").forward(req, res);
            return;
        }

        String expectedToken =
                (String) session.getAttribute("csrfToken");
        if (expectedToken == null
                || !expectedToken.equals(
                        req.getParameter("csrfToken"))) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        long id;
        try {
            id = Long.parseLong(req.getParameter("id"));
        } catch (NumberFormatException e) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String deletedName = new WorriedTypeDao().deactivate(
                id, user.getId());
        if (deletedName == null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        session.setAttribute(
                "principalDangerTypeDeleted", deletedName);
        res.setStatus(HttpServletResponse.SC_SEE_OTHER);
        res.setHeader(
                "Location",
                req.getContextPath()
                        + "/safety/principal/DangerTypeDelete.action");
    }
}
