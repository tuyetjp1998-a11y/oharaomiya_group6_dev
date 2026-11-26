package safety.principal;

import bean.User;
import dao.PrincipalStaffDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;
import tool.PasswordUtil;
import java.nio.charset.StandardCharsets;

public class StaffEditSubmitAction extends Action {
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
        long id;
        try { id = Long.parseLong(req.getParameter("teacherId")); if (id <= 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { res.sendError(400); return; }
        PrincipalStaffDao dao = new PrincipalStaffDao();
        var staff = dao.findById(id);
        if (staff == null) { res.sendError(404); return; }
        String name = req.getParameter("name");
        name = name == null ? "" : name.strip();
        String password = req.getParameter("password");
        String activeParam = req.getParameter("active");
        if (activeParam != null && !"true".equals(activeParam)) { res.sendError(400); return; }
        boolean active = "true".equals(activeParam);
        String error;
        if (name.isBlank() || name.length() > 40 || name.indexOf('\0') >= 0) {
            error = "氏名を40文字以内で入力してください。";
        } else if (password != null && !password.isEmpty() && (password.length() < 8 || password.isBlank()
                || password.indexOf('\0') >= 0 || password.getBytes(StandardCharsets.UTF_8).length > 72)) {
            error = "パスワードは8文字以上、UTF-8で72バイト以内で入力してください。";
        } else {
            try {
                String hash = password == null || password.isEmpty() ? null : PasswordUtil.hash(password);
                if (dao.updateStaff(user.getId(), id, name, hash, active)) {
                    session.setAttribute("staffEditDone", Boolean.TRUE);
                    res.setStatus(HttpServletResponse.SC_SEE_OTHER);
                    res.setHeader("Location", req.getContextPath() + "/safety/principal/StaffEditDone.action");
                    return;
                }
                error = "職員情報を更新できませんでした。";
            } catch (java.sql.SQLException | javax.naming.NamingException e) {
                req.getServletContext().log("職員更新に失敗しました。" + e.getClass().getSimpleName());
                error = "職員情報の更新に失敗しました。管理者にお問い合わせください。";
            }
        }
        req.setAttribute("staff", staff);
        req.setAttribute("staffName", name);
        req.setAttribute("staffActive", active);
        req.setAttribute("staffError", error);
        req.getRequestDispatcher("/WEB-INF/views/principal/staff_edit.jsp").forward(req, res);
    }
}
