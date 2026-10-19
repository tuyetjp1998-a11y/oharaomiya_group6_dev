package safety.teacher;

import java.time.LocalDate;
import java.time.ZoneId;

import bean.User;
import dao.FamilyRequestReviewDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * 親子関係申請を承認または却下する。
 */
public class FamilyRequestReviewAction extends Action {

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

        long requestId;
        String expectedToken;
        boolean reviewed;

        // リクエストパラメーターの取得 2
        req.setCharacterEncoding("UTF-8");

        String requestIdParam = req.getParameter("requestId");
        String decision = req.getParameter("decision");
        String csrfToken = req.getParameter("csrfToken");

        // DBからデータ取得 3
        // DAO内で申請と担当権限を確認する

        // ビジネスロジック 4
        res.setHeader("Cache-Control", "no-store");

        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "POST");
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

        expectedToken =
                (String) session.getAttribute("csrfToken");

        if (expectedToken == null
                || csrfToken == null
                || !expectedToken.equals(csrfToken)) {
            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            requestId = Long.parseLong(requestIdParam);

            if (requestId <= 0) {
                res.sendError(
                        HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        } catch (NumberFormatException e) {
            res.sendError(
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (!"APPROVED".equals(decision)
                && !"REJECTED".equals(decision)) {
            res.sendError(
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        LocalDate today = LocalDate.now(
                ZoneId.of("Asia/Tokyo"));

        int schoolYear = today.getMonthValue() >= 4
                ? today.getYear()
                : today.getYear() - 1;

        // DBへデータ保存 5
        reviewed = dao.review(
                requestId, user.getId(), schoolYear, decision);

        if (!reviewed) {
            res.sendError(
                    HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // レスポンス値をセット 6
        session.setAttribute(
                "reviewMessage",
                "APPROVED".equals(decision)
                        ? "申請を承認しました。"
                        : "申請を却下しました。");

        // 一覧画面へリダイレクト 7
        res.setStatus(HttpServletResponse.SC_SEE_OTHER);
        res.setHeader(
                "Location",
                req.getContextPath()
                + "/safety/teacher/FamilyRequests.action");
    }
}