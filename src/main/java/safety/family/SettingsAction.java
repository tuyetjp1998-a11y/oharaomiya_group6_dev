package safety.family;

import java.util.List;

import bean.FamilyStudent;
import bean.FamilyStudentRequest;
import bean.User;
import dao.FamilyStudentDao;
import dao.FamilyStudentRequestDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * 保護者の設定画面を表示する。
 */
public class SettingsAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        User user = session == null
                ? null
                : (User) session.getAttribute("user");

        FamilyStudentDao dao = new FamilyStudentDao();
        List<FamilyStudent> students;
        
        FamilyStudentRequestDao requestDao =
                new FamilyStudentRequestDao();
        List<FamilyStudentRequest> pendingRequests;
        List<FamilyStudentRequest> reviewedRequests;
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
            res.sendRedirect(
                    req.getContextPath()
                    + "/safety/auth/Login.action");
            return;
        }

        if (!user.isActive()
                || !"FAMILY".equals(user.getRole())) {
            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // ログイン中の保護者に関連付けられた生徒を取得
        students = dao.findByFamilyUserId(user.getId());
        pendingRequests =
                requestDao.findPendingByFamilyUserId(user.getId());
        reviewedRequests =
                requestDao.findReviewedByFamilyUserId(user.getId());
        // DBへデータ保存 5
        // なし

        // レスポンス値をセット 6
        req.setAttribute("students", students);
        req.setAttribute("pendingRequests", pendingRequests);
        req.setAttribute("reviewedRequests", reviewedRequests);
        // POST処理の結果を一度だけ表示する
        req.setAttribute(
             "childRequestMessage",
             session.getAttribute("childRequestMessage"));

     session.removeAttribute("childRequestMessage");
        // JSPへフォワード 7
        req.getRequestDispatcher(
                "/WEB-INF/views/family/settings.jsp")
                .forward(req, res);
    }
}