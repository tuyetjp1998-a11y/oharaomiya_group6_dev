package safety.family;

import bean.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * お子さんの追加申請画面を表示する。
 */
public class ChildAddAction extends Action {

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

        if (user == null) {
            res.sendRedirect(req.getContextPath()
                    + "/safety/auth/Login.action");
            return;
        }

        if (!user.isActive()
                || !"FAMILY".equals(user.getRole())) {
            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // DBへデータ保存 5
        // なし

        // レスポンス値をセット 6
        req.setAttribute(
                "childRequestMessage",
                session.getAttribute("childRequestMessage"));
        session.removeAttribute("childRequestMessage");

        // JSPへフォワード 7
        req.getRequestDispatcher(
                "/WEB-INF/views/family/child_add.jsp")
                .forward(req, res);
    }
}