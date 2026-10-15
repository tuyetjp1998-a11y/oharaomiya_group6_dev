package safety.student;

import bean.User;
import dao.UserDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * 生徒の位置情報利用設定を保存するAction。
 */
public class SettingsSaveAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        User user = null;
        UserDao dao = new UserDao();
        boolean enabled;

        // リクエストパラメーターの取得 2
        req.setCharacterEncoding("UTF-8");

        String csrfToken = req.getParameter("csrfToken");
        String enabledParam =
                req.getParameter("locationEnabled");

        // DBからデータ取得 3
        // なし

        // ビジネスロジック 4
        res.setHeader("Cache-Control", "no-store");

        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "POST");
            res.sendError(
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        if (session != null) {
            user = (User) session.getAttribute("user");
        }

        if (user == null) {
            res.sendRedirect(req.getContextPath()
                    + "/safety/auth/Login.action");
            return;
        }

        if (!user.isActive()
                || !"STUDENT".equals(user.getRole())) {
            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String expectedToken =
                (String) session.getAttribute("csrfToken");

        if (expectedToken == null
                || !expectedToken.equals(csrfToken)) {
            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (!"true".equals(enabledParam)
                && !"false".equals(enabledParam)) {
            res.sendError(
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        enabled = Boolean.parseBoolean(enabledParam);

        // DBへデータ保存 5
        if (!dao.updateLocationEnabled(user.getId(), enabled)) {
            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // レスポンス値をセット 6
        user.setLocationEnabled(enabled);
        session.setAttribute("user", user);
        session.setAttribute(
                "settingsSaved",
                Boolean.TRUE);

        // 設定画面へリダイレクト 7
        String settingsUrl = req.getContextPath()
                + "/safety/student/Settings.action";

        res.setStatus(HttpServletResponse.SC_SEE_OTHER);
        res.setHeader(
                "Location",
                res.encodeRedirectURL(settingsUrl));
    }
}