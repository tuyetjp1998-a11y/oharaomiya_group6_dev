package safety.principal;

import bean.User;
import dao.PrincipalGroupDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

public class GroupCreateAction extends Action {
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
        String name = req.getParameter("className");
        PrincipalGroupDao dao = new PrincipalGroupDao();
        String error;
        try {
            if (dao.createClass(name)) {
                session.setAttribute("groupCreateDone", Boolean.TRUE);
                res.setStatus(HttpServletResponse.SC_SEE_OTHER);
                res.setHeader("Location", req.getContextPath() + "/safety/principal/GroupCreateDone.action");
                return;
            }
            error = "同じクラス名が既に登録されています。";
        } catch (IllegalArgumentException e) {
            error = e.getMessage();
        } catch (java.sql.SQLException | javax.naming.NamingException e) {
            req.getServletContext().log("クラス登録に失敗しました。", e);
            error = "クラスの作成に失敗しました。管理者にお問い合わせください。";
        }
        req.setAttribute("groupError", error);
        req.setAttribute("className", name);
        req.setAttribute("groups", dao.findGroups());
        req.getRequestDispatcher("/WEB-INF/views/principal/groups.jsp").forward(req, res);
    }
}
