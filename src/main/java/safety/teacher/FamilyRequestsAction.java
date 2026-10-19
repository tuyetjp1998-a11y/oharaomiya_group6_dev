package safety.teacher;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import bean.FamilyRequestReview;
import bean.User;
import dao.FamilyRequestReviewDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * 担当クラスの親子関係申請一覧を表示する。
 */
public class FamilyRequestsAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        User user = session == null
                ? null
                : (User) session.getAttribute("user");

        FamilyRequestReviewDao dao =
                new FamilyRequestReviewDao();
        List<FamilyRequestReview> requests;

        // リクエストパラメーターの取得 2
        // なし

        // DBからデータ取得 3
        // 認証確認後に取得する

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
                || !"TEACHER".equals(user.getRole())) {
            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        LocalDate today = LocalDate.now(
                ZoneId.of("Asia/Tokyo"));

        int schoolYear = today.getMonthValue() >= 4
                ? today.getYear()
                : today.getYear() - 1;

        requests = dao.findPending(user.getId(), schoolYear);

        // DBへデータ保存 5
        // なし

        // レスポンス値をセット 6
        req.setAttribute("requests", requests);
        req.setAttribute("schoolYear", schoolYear);

        req.setAttribute(
                "reviewMessage",
                session.getAttribute("reviewMessage"));
        session.removeAttribute("reviewMessage");

        // JSPへフォワード 7
        req.getRequestDispatcher(
                "/WEB-INF/views/teacher/family_requests.jsp")
                .forward(req, res);
    }
}