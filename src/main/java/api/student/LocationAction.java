package api.student;

import java.sql.SQLException;
import java.util.Map;

import javax.naming.NamingException;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import bean.SafetyReport;
import bean.User;
import dao.SafetyReportDao;
import dao.WorriedTypeDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiAuthUtil;
import tool.ApiJsonUtil;

/**
 * 生徒本人が最後に送信した回答の位置情報を返す。
 */
public class LocationAction extends Action {

    @Override
    public void execute(
            HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        res.setHeader("Cache-Control", "no-store");

        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");

            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    Map.of(
                            "error", "METHOD_NOT_ALLOWED",
                            "message", "GETでアクセスしてください。"));
            return;
        }

        // 他の生徒のIDをリクエストで受け付けない。
        User student = ApiAuthUtil.requireRole(
                req, res, "STUDENT");

        if (student == null) {
            return;
        }

        try {
            SafetyReport report = new SafetyReportDao()
                    .getLatestByUserId(student.getId());

            JsonObject result = new JsonObject();

            if (report == null) {
                result.add("latestReport", JsonNull.INSTANCE);

            } else {
                JsonObject reportJson = new JsonObject();

                reportJson.addProperty(
                        "reportId",
                        report.getId());

                // 地図と同じ回答の安否状況・種類を返す。
                reportJson.addProperty(
                        "status",
                        report.getStatus());

                reportJson.addProperty(
                        "dangerType",
                        report.getDangerType());

                // 選択肢から削除済みでも、過去の回答の名称を返す。
                String dangerTypeName = "WORRIED".equals(report.getStatus())
                        ? new WorriedTypeDao().findNameByCode(report.getDangerType())
                        : null;
                reportJson.addProperty("dangerTypeName", dangerTypeName);

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
            // 位置情報や認証情報はログに出力しない。
            req.getServletContext().log(
                    "API生徒位置情報の取得に失敗しました。例外="
                            + e.getClass().getSimpleName());

            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    Map.of(
                            "error", "SERVICE_UNAVAILABLE",
                            "message",
                            "現在利用できません。時間をおいてお試しください。"));
        }
    }
}