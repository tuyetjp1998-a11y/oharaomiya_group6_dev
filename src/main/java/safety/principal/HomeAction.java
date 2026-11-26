package safety.principal;

import bean.User;
import dao.PrincipalSafetyDao;
import dao.PrincipalSafetyDao.HomeData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

public class HomeAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        User user = session == null
                ? null
                : (User) session.getAttribute("user");

        // リクエストパラメーターの取得 2
        // なし

        // ビジネスロジック 4
        res.setHeader("Cache-Control", "no-store");

        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");
            res.sendError(
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        if (user == null) {
            res.sendRedirect(req.getContextPath()
                    + "/safety/auth/Login.action");
            return;
        }

        if (!user.isActive()
                || !"PRINCIPAL".equals(user.getRole())) {
            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // DBから全校の最新安否状況を取得（校長権限の確認後）
        PrincipalSafetyDao dao = new PrincipalSafetyDao();
        HomeData homeData = dao.findHomeData();

        // DBへデータ保存 5（参照のみ）

        // レスポンス値をセット 6
        req.setAttribute("pageTitle", "管理ホーム");
        req.setAttribute("homeData", homeData);

        // JSPへフォワード 7
        req.getRequestDispatcher(
                "/WEB-INF/views/principal/home.jsp")
                .forward(req, res);
    }
}
