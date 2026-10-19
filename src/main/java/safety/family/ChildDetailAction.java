package safety.family;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import bean.FamilyStudent;
import bean.SafetyReport;
import bean.User;
import dao.FamilyStudentDao;
import dao.SafetyReportDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * 関連付けられた生徒の最新の安否回答を表示する。
 */
public class ChildDetailAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        User user = session == null
                ? null
                : (User) session.getAttribute("user");

        FamilyStudentDao familyStudentDao =
                new FamilyStudentDao();
        SafetyReportDao reportDao = new SafetyReportDao();

        FamilyStudent student;
        SafetyReport report;
        long studentUserId;
        String reportedAtText = null;

        // リクエストパラメーターの取得 2
        String studentIdParam = req.getParameter("studentId");

        // DBからデータ取得 3
        // 認証とパラメーター検証後に取得する

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

        try {
            studentUserId = Long.parseLong(studentIdParam);

            if (studentUserId <= 0) {
                res.sendError(
                        HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        } catch (NumberFormatException e) {
            res.sendError(
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // ログイン中の保護者と生徒の関連付けを確認
        student = familyStudentDao.findLinkedStudent(
                user.getId(), studentUserId);

        if (student == null) {
            res.sendError(
                    HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 関連付けを確認できた生徒の回答だけを取得
        report = reportDao.getLatestByUserId(studentUserId);

        if (report != null) {
            reportedAtText = report.getReportedAt()
                    .atZoneSameInstant(ZoneId.of("Asia/Tokyo"))
                    .format(DateTimeFormatter.ofPattern(
                            "yyyy/MM/dd HH:mm:ss"));
        }

        // DBへデータ保存 5
        // なし

        // レスポンス値をセット 6
        req.setAttribute("student", student);
        req.setAttribute("report", report);
        req.setAttribute("reportedAtText", reportedAtText);

        // JSPへフォワード 7
        req.getRequestDispatcher(
                "/WEB-INF/views/family/child_detail.jsp")
                .forward(req, res);
    }
}