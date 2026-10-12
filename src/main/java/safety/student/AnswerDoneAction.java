package safety.student;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import bean.SafetyReport;
import bean.User;
import dao.SafetyReportDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * 保存済みの安否回答を完了画面に表示するAction。
 */
public class AnswerDoneAction extends Action {

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res)
            throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        User user = null;
        SafetyReportDao dao = new SafetyReportDao();
        SafetyReport report;
        long id;
        String statusLabel;
        String dangerLabel = null;

        // リクエストパラメーターの取得 2
        String idParam = req.getParameter("id");

        // DBからデータ取得 3
        // 認証とパラメーター検証後に取得する

        // ビジネスロジック 4
        res.setHeader("Cache-Control", "no-store");

        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        if (session != null) {
            user = (User) session.getAttribute("user");
        }

        if (user == null) {
            res.sendRedirect(
                    req.getContextPath() + "/safety/auth/Login.action");
            return;
        }

        if (!user.isActive()
                || !"STUDENT".equals(user.getRole())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            id = Long.parseLong(idParam);

            if (id <= 0) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        } catch (NumberFormatException e) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // 自分が登録した回答だけを取得
        report = dao.get(id, user.getId());

        if (report == null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if ("SAFE".equals(report.getStatus())) {
            statusLabel = "無事";
        } else {
            statusLabel = "危険";

            switch (report.getDangerType()) {
                case "ACCIDENT":
                    dangerLabel = "事故";
                    break;
                case "EARTHQUAKE":
                    dangerLabel = "地震";
                    break;
                case "FIRE":
                    dangerLabel = "火災";
                    break;
                case "LANDSLIDE":
                    dangerLabel = "土砂崩れ";
                    break;
                case "STALKING":
                    dangerLabel = "付きまとわれている";
                    break;
                default:
                    throw new IllegalStateException(
                            "不正な危険の種類です。");
            }
        }

        String reportedAtText = report.getReportedAt()
                .atZoneSameInstant(ZoneId.of("Asia/Tokyo"))
                .format(DateTimeFormatter.ofPattern(
                        "yyyy/MM/dd HH:mm:ss"));

        // DBへデータ保存 5
        // なし

        // レスポンス値をセット 6
        req.setAttribute("report", report);
        req.setAttribute("statusLabel", statusLabel);
        req.setAttribute("dangerLabel", dangerLabel);
        req.setAttribute("reportedAtText", reportedAtText);

        // JSPへフォワード 7
        req.getRequestDispatcher(
                "/WEB-INF/views/student/answer_done.jsp")
                .forward(req, res);
    }
}