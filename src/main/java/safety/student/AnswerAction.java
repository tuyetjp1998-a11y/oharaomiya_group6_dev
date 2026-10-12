package safety.student;

import java.util.UUID;

import bean.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

public class AnswerAction extends Action {

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res)
            throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        User user = null;

        // リクエストパラメーターの取得 2
        // なし

        // DBからデータ取得 3
        // なし

        // ビジネスロジック 4
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
            res.sendRedirect(
                    req.getContextPath()
                    + "/safety/auth/Login.action");
            return;
        }

        if (!user.isActive()
                || !"STUDENT".equals(user.getRole())) {
            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // DBへデータ保存 5
        // なし
        
     // 回答フォーム用のCSRFトークンを準備
        synchronized (session) {
            if (session.getAttribute("answerCsrfToken") == null) {
                session.setAttribute(
                        "answerCsrfToken",
                        UUID.randomUUID().toString());
            }
        }

        // レスポンス値をセット 6
        res.setHeader("Cache-Control", "no-store");

        // JSPへフォワード 7
        req.getRequestDispatcher(
                "/WEB-INF/views/student/answer.jsp")
                .forward(req, res);
    }
}