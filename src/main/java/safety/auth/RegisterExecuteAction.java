package safety.auth;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;

import dao.UserDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;
import tool.PasswordUtil;

/**
 * 保護者アカウントと親子関係の追加申請を登録する。
 */
public class RegisterExecuteAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        UserDao dao = new UserDao();
        String errorMessage = null;
        boolean registered = false;

        // リクエストパラメーターの取得 2
        req.setCharacterEncoding("UTF-8");

        String name = req.getParameter("name");
        String loginId = req.getParameter("loginId");
        String password = req.getParameter("password");
        String studentLoginId =
                req.getParameter("studentLoginId");
        String csrfToken = req.getParameter("csrfToken");

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

        String expectedToken = session == null
                ? null
                : (String) session.getAttribute("csrfToken");

        if (expectedToken == null
                || csrfToken == null
                || !expectedToken.equals(csrfToken)) {

            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "登録画面を開き直してください。");
            return;
        }

        // 氏名とIDの前後の空白を除去
        name = name == null ? "" : name.strip();
        loginId = loginId == null ? "" : loginId.strip();
        studentLoginId = studentLoginId == null
                ? ""
                : studentLoginId.strip();

        if (name.isBlank() || name.length() > 40) {
            errorMessage =
                    "氏名は1～40文字で入力してください。";

        } else if (loginId.isBlank()
                || loginId.length() > 40) {
            errorMessage =
                    "ユーザーIDは1～40文字で入力してください。";

        } else if (password == null
                || password.isEmpty()
                || password.getBytes(
                        StandardCharsets.UTF_8).length > 72) {
            errorMessage =
                    "パスワードはUTF-8で1～72バイトにしてください。";

        } else if (studentLoginId.isBlank()
                || studentLoginId.length() > 40) {
            errorMessage =
                    "お子さんの生徒IDは1～40文字で入力してください。";
        }

        LocalDate today =
                LocalDate.now(ZoneId.of("Asia/Tokyo"));

        int schoolYear = today.getMonthValue() >= 4
                ? today.getYear()
                : today.getYear() - 1;

        // DBへデータ保存 5
        if (errorMessage == null) {
            String passwordHash = PasswordUtil.hash(password);

            registered = dao.registerFamily(
                    loginId,
                    name,
                    passwordHash,
                    studentLoginId,
                    schoolYear);

            if (!registered) {
                errorMessage =
                        "登録できませんでした。ユーザーIDが登録済みでないか、"
                        + "お子さんの生徒IDや所属情報を確認してください。";
            }
        }

        // レスポンス値をセット 6
        if (registered) {
            session.setAttribute("registerCompleted", true);

        } else {
            req.setAttribute("registerError", errorMessage);
            req.setAttribute("inputName", name);
            req.setAttribute("inputLoginId", loginId);
            req.setAttribute(
                    "inputStudentLoginId", studentLoginId);

            // パスワードは画面やセッションに保存しない
        }

        // JSPへフォワード 7
        if (registered) {
            String doneUrl = req.getContextPath()
                    + "/safety/auth/RegisterDone.action";

            res.setStatus(HttpServletResponse.SC_SEE_OTHER);
            res.setHeader(
                    "Location",
                    res.encodeRedirectURL(doneUrl));

        } else {
            req.getRequestDispatcher(
                    "/WEB-INF/views/auth/register.jsp")
                    .forward(req, res);
        }
    }
}