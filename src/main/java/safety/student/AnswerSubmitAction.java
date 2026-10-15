package safety.student;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Set;

import bean.SafetyReport;
import bean.User;
import dao.SafetyReportDao;
import dao.UserDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/**
 * 安否回答と位置情報を登録するAction。
 */
public class AnswerSubmitAction extends Action {

    private static final Set<String> DANGER_TYPES = Set.of(
            "ACCIDENT",
            "EARTHQUAKE",
            "FIRE",
            "LANDSLIDE",
            "STALKING");

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        User user = null;
        SafetyReport report = new SafetyReport();
        SafetyReportDao dao = new SafetyReportDao();
        String sessionToken;
        boolean saved;

        // リクエストパラメーターの取得 2
        req.setCharacterEncoding("UTF-8");

        String status = req.getParameter("status");
        String dangerType = req.getParameter("dangerType");
        String csrfToken = req.getParameter("csrfToken");

        String latitudeParam = req.getParameter("latitude");
        String longitudeParam = req.getParameter("longitude");
        String accuracyParam =
                req.getParameter("locationAccuracy");
        String acquiredAtParam =
                req.getParameter("locationAcquiredAt");

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

        sessionToken =
                (String) session.getAttribute("answerCsrfToken");

        if (sessionToken == null
                || !sessionToken.equals(csrfToken)) {
            res.sendError(
                    HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // 安否状況と危険の種類を検証
        if ("SAFE".equals(status)) {
            if (!isBlank(dangerType)) {
                res.sendError(
                        HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            dangerType = null;

        } else if ("DANGER".equals(status)) {
            if (dangerType == null
                    || !DANGER_TYPES.contains(dangerType)) {
                res.sendError(
                        HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

        } else {
            res.sendError(
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

	     // フォームの値ではなくDBの最新設定を使用
	        boolean locationEnabled =
	                new UserDao().getLocationEnabled(user.getId());
	
	        boolean noLocation =
	                !locationEnabled
	                || (
	                    isBlank(latitudeParam)
	                    && isBlank(longitudeParam)
	                    && isBlank(accuracyParam)
	                    && isBlank(acquiredAtParam)
	                );

        if (!noLocation) {
            if (isBlank(latitudeParam)
                    || isBlank(longitudeParam)
                    || isBlank(accuracyParam)
                    || isBlank(acquiredAtParam)) {
                res.sendError(
                        HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            try {
                double latitude =
                        Double.parseDouble(latitudeParam);
                double longitude =
                        Double.parseDouble(longitudeParam);
                double accuracy =
                        Double.parseDouble(accuracyParam);

                OffsetDateTime acquiredAt =
                        OffsetDateTime.parse(acquiredAtParam);

                if (!Double.isFinite(latitude)
                        || latitude < -90
                        || latitude > 90
                        || !Double.isFinite(longitude)
                        || longitude < -180
                        || longitude > 180
                        || !Double.isFinite(accuracy)
                        || accuracy < 0) {
                    res.sendError(
                            HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                report.setLatitude(latitude);
                report.setLongitude(longitude);
                report.setLocationAccuracy(accuracy);
                report.setLocationAcquiredAt(acquiredAt);

            } catch (NumberFormatException
                    | DateTimeParseException e) {
                res.sendError(
                        HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }

        // 回答者はセッションから取得
        report.setUserId(user.getId());
        report.setStatus(status);
        report.setDangerType(dangerType);

        // DBへデータ保存 5
        saved = dao.save(report);

        if (!saved) {
            res.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // レスポンス値をセット 6
        String doneUrl = req.getContextPath()
                + "/safety/student/AnswerDone.action?id="
                + report.getId();

        // 完了画面へリダイレクト 7
        res.setStatus(HttpServletResponse.SC_SEE_OTHER);
        res.setHeader(
                "Location",
                res.encodeRedirectURL(doneUrl));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}