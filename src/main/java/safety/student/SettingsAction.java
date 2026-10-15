package safety.student;

import bean.User;
import dao.UserDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * 生徒の設定画面を表示するAction。
 */
public class SettingsAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        User user = null;

        // リクエストパラメーターの取得 2
        // なし

        // DBからデータ取得 3
        // なし

        // ビジネスロジック 4
        res.setHeader("Cache-Control", "no-store");

        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");
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
        
     // DBから最新の設定を取得
        boolean locationEnabled =
                new UserDao().getLocationEnabled(user.getId());

        user.setLocationEnabled(locationEnabled);
        req.setAttribute("locationEnabled", locationEnabled);
        // DBへデータ保存 5
        // なし

        // レスポンス値をセット 6
        if (Boolean.TRUE.equals(
                session.getAttribute("settingsSaved"))) {

            req.setAttribute("settingsSaved", true);
            session.removeAttribute("settingsSaved");
        }

        // JSPへフォワード 7
        req.getRequestDispatcher(
                "/WEB-INF/views/student/settings.jsp")
                .forward(req, res);
    }
}