package safety.family;

import java.time.LocalDate;
import java.time.ZoneId;

import bean.User;
import dao.FamilyStudentRequestDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * 生徒との関連付け申請を受け付ける。
 */
public class ChildRequestAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        User user = session == null
                ? null
                : (User) session.getAttribute("user");

        FamilyStudentRequestDao dao =
                new FamilyStudentRequestDao();

        String expectedToken;
        String message;
        boolean created;

        // リクエストパラメーターの取得 2
        req.setCharacterEncoding("UTF-8");

        String studentLoginId =
                req.getParameter("studentLoginId");
        String csrfToken = req.getParameter("csrfToken");

        // DBからデータ取得 3
        // 登録SQLで対象者と関連付けの状態を確認する

        // ビジネスロジック 4
        res.setHeader("Cache-Control", "no-store");

        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "POST");
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

        expectedToken =
                (String) session.getAttribute("csrfToken");

        if (expectedToken == null
                || csrfToken == null
                || !expectedToken.equals(csrfToken)) {
            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (studentLoginId != null) {
            studentLoginId = studentLoginId.strip();
        }

        if (studentLoginId == null
                || studentLoginId.isBlank()
                || studentLoginId.length() > 100) {

            session.setAttribute(
                    "childRequestMessage",
                    "生徒IDを1～100文字で入力してください。");

            res.setStatus(HttpServletResponse.SC_SEE_OTHER);
            res.setHeader(
                    "Location",
                    req.getContextPath()
                    + "/safety/family/Settings.action");
            return;
        }

        LocalDate today = LocalDate.now(
                ZoneId.of("Asia/Tokyo"));

        int schoolYear = today.getMonthValue() >= 4
                ? today.getYear()
                : today.getYear() - 1;

        // DBへデータ保存 5
        created = dao.create(
                user.getId(), studentLoginId, schoolYear);

        // レスポンス値をセット 6
        if (created) {
            message = "申請を受け付けました。教員の承認をお待ちください。";
        } else {
            message = "申請できませんでした。"
                    + "登録済み・申請中でないか、生徒IDや所属情報を確認してください。";
        }

        session.setAttribute("childRequestMessage", message);

        // 設定画面へリダイレクト 7
        res.setStatus(HttpServletResponse.SC_SEE_OTHER);
        res.setHeader(
                "Location",
                req.getContextPath()
                + "/safety/family/Settings.action");
    }
}