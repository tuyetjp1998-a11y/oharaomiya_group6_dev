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
 * 最新の安否回答に登録された位置情報を表示するAction。
 */
public class LocationAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        User user = null;
        SafetyReportDao dao = new SafetyReportDao();
        SafetyReport latestReport;
        String locationAcquiredAtText = null;
        String reportedAtText = null;

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "yyyy/MM/dd HH:mm:ss");

        ZoneId japanZone = ZoneId.of("Asia/Tokyo");

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

        if (session != null) {
            user = (User) session.getAttribute("user");
        }

        if (user == null) {
            res.sendRedirect(req.getContextPath()
                    + "/safety/auth/Login.action");
            return;
        }

        if (!user.isActive()
                || !"STUDENT".equals(user.getRole())) {
            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // ログイン中の利用者の最新回答のみ取得
        latestReport = dao.getLatestByUserId(user.getId());

        if (latestReport != null) {
            reportedAtText = latestReport.getReportedAt()
                    .atZoneSameInstant(japanZone)
                    .format(formatter);

            if (latestReport.getLocationAcquiredAt() != null) {
                locationAcquiredAtText =
                        latestReport.getLocationAcquiredAt()
                                .atZoneSameInstant(japanZone)
                                .format(formatter);
            }
        }

        // DBへデータ保存 5
        // なし

        // レスポンス値をセット 6
        req.setAttribute("latestReport", latestReport);
        req.setAttribute("reportedAtText", reportedAtText);
        req.setAttribute(
                "locationAcquiredAtText",
                locationAcquiredAtText);

        // JSPへフォワード 7
        req.getRequestDispatcher(
                "/WEB-INF/views/student/location.jsp")
                .forward(req, res);
    }
}