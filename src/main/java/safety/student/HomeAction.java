package safety.student;

import bean.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import safety.auth.NotificationIntroAction;
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

        // DBからデータ取得 3
        // なし

        // ビジネスロジック 4
        if (user == null) {
            res.sendRedirect(req.getContextPath()
                    + "/safety/auth/Login.action");
            return;
        }

        if (!user.isActive()
                || !"STUDENT".equals(user.getRole())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        req.setAttribute("checkNotificationIntro",
                !NotificationIntroAction.wasShownToday(req, user));

        // DBへデータ保存 5
        // なし

        // レスポンス値をセット 6
     // レスポンス値をセット 6
        res.setHeader("Cache-Control", "no-store");
        req.setAttribute("pageTitle", "生徒トップ");

        // 通知登録に使用する公開鍵を画面へ渡す
        String vapidPublicKey =
                System.getenv("VAPID_PUBLIC_KEY");

        req.setAttribute(
                "vapidPublicKey",
                vapidPublicKey == null
                        ? ""
                        : vapidPublicKey.strip());

        // JSPへフォワード 7
        req.getRequestDispatcher(
                "/WEB-INF/views/student/home.jsp")
                .forward(req, res);
    }
}