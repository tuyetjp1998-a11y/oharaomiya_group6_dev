package api.family;

import java.sql.SQLException;
import java.util.Map;

import javax.naming.NamingException;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import bean.FamilyStudent;
import bean.SafetyReport;
import bean.User;
import dao.FamilyStudentDao;
import dao.SafetyReportDao;
import dao.WorriedTypeDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiAuthUtil;
import tool.ApiJsonUtil;

/**
 * 関連付けられた生徒の最新の安否回答を返す。
 */
public class StudentStatusAction extends Action {

    @Override
    public void execute(
            HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        res.setHeader("Cache-Control", "no-store");

        // GET以外は受け付けない
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");

            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    Map.of("error", "METHOD_NOT_ALLOWED"));
            return;
        }

        User family = ApiAuthUtil.requireRole(
                req, res, "FAMILY");

        if (family == null) {
            return;
        }

        // 生徒IDは1つだけ指定する
        String[] values =
                req.getParameterValues("studentUserId");

        if (values == null
                || values.length != 1
                || !values[0].matches("[0-9]{1,19}")) {

            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("error", "INVALID_INPUT"));
            return;
        }

        long studentUserId;

        try {
            studentUserId = Long.parseLong(values[0]);

            if (studentUserId <= 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {
            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("error", "INVALID_INPUT"));
            return;
        }

        try {
            // 保護者IDはアクセストークンから取得する
            FamilyStudent student = new FamilyStudentDao()
                    .findLinkedStudent(
                            family.getId(),
                            studentUserId);

            // 未関連の生徒と存在しない生徒は同じ応答にする
            if (student == null) {
                ApiJsonUtil.write(
                        res,
                        HttpServletResponse.SC_NOT_FOUND,
                        Map.of("error", "NOT_FOUND"));
                return;
            }

            SafetyReport report = new SafetyReportDao()
                    .getLatestByUserId(student.getStudentUserId());

            JsonObject studentJson = new JsonObject();

            studentJson.addProperty(
                    "studentUserId",
                    student.getStudentUserId());

            studentJson.addProperty(
                    "studentName",
                    student.getStudentName());

            JsonObject result = new JsonObject();
            result.add("student", studentJson);

            if (report == null) {
                // 関連付け済みでも未回答の場合がある
                result.add("latestReport", JsonNull.INSTANCE);

            } else {
                JsonObject reportJson = new JsonObject();

                reportJson.addProperty(
                        "reportId",
                        report.getId());

                reportJson.addProperty(
                        "status",
                        report.getStatus());

                reportJson.addProperty(
                        "dangerType",
                        report.getDangerType());

                // 論理削除済みの項目も過去の回答には表示する。
                String dangerTypeName = null;
                if ("WORRIED".equals(report.getStatus())
                        && report.getDangerType() != null) {
                    dangerTypeName = new WorriedTypeDao()
                            .findNameByCode(report.getDangerType());
                }
                reportJson.addProperty("dangerTypeName", dangerTypeName);
                reportJson.addProperty(
                        "comment",
                        report.getComment());

                reportJson.addProperty(
                        "reportedAt",
                        report.getReportedAt().toString());

                reportJson.addProperty(
                        "latitude",
                        report.getLatitude());

                reportJson.addProperty(
                        "longitude",
                        report.getLongitude());

                reportJson.addProperty(
                        "locationAccuracy",
                        report.getLocationAccuracy());

                reportJson.addProperty(
                        "locationAcquiredAt",
                        report.getLocationAcquiredAt() == null
                                ? null
                                : report.getLocationAcquiredAt()
                                        .toString());

                result.add("latestReport", reportJson);
            }

            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_OK,
                    result);

        } catch (NamingException | SQLException e) {
            // 生徒情報や位置情報、接続情報は記録しない
            req.getServletContext().log(
                    "API安否詳細の取得に失敗しました。例外="
                            + e.getClass().getSimpleName());

            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    Map.of("error", "SERVICE_UNAVAILABLE"));
        }
    }
}