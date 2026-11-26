package safety.principal;

import bean.User;
import dao.PrincipalStaffDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;
import tool.PasswordUtil;
import java.nio.charset.StandardCharsets;

public class StaffAddSubmitAction extends Action {
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
        String name = req.getParameter("name");
        String loginId = req.getParameter("loginId");
        String password = req.getParameter("password");
        name = name == null ? "" : name.strip();
        loginId = loginId == null ? "" : loginId.strip();
        String error;
        if (name.isBlank() || name.length() > 40 || name.indexOf('\0') >= 0) {
            error = "氏名を40文字以内で入力してください。";
        } else if (!loginId.matches("[A-Za-z0-9._-]{1,40}")) {
            error = "ユーザーIDは40文字以内の半角英数字・記号（. _ -）で入力してください。";
        } else if (password == null || password.length() < 8 || password.isBlank()
                || password.indexOf('\0') >= 0 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            error = "初期パスワードは8文字以上、UTF-8で72バイト以内で入力してください。";
        } else {
            try {
                String hash = PasswordUtil.hash(password);
                if (new PrincipalStaffDao().createStaff(user.getId(), name, loginId, hash)) {
                    session.setAttribute("staffAddDone", Boolean.TRUE);
                    res.setStatus(HttpServletResponse.SC_SEE_OTHER);
                    res.setHeader("Location", req.getContextPath() + "/safety/principal/StaffAddDone.action");
                    return;
                }
                error = "ユーザーIDが既に登録されているか、登録権限がありません。";
            } catch (java.sql.SQLException | javax.naming.NamingException e) {
                req.getServletContext().log("職員登録に失敗しました。" + e.getClass().getSimpleName());
                error = "職員の登録に失敗しました。管理者にお問い合わせください。";
            }
        }
        req.setAttribute("staffError", error);
        req.setAttribute("staffName", name);
        req.setAttribute("staffLoginId", loginId);
        req.getRequestDispatcher("/WEB-INF/views/principal/staff_add.jsp").forward(req, res);
    }
}
