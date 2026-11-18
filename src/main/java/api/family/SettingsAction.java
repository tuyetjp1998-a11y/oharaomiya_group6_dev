package api.family;

import java.sql.SQLException;
import java.util.Map;
import javax.naming.NamingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import dao.FamilyNotificationSettingsDao;
import dao.FamilyStudentDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiAuthUtil;
import tool.ApiJsonUtil;

public class SettingsAction extends Action {
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        res.setHeader("Cache-Control", "no-store");
        boolean update = "POST".equals(req.getMethod());
        if (!update && !"GET".equals(req.getMethod())) {
            res.setHeader("Allow", "GET, POST");
            error(res, 405, "METHOD_NOT_ALLOWED", "GETまたはPOSTでアクセスしてください。");
            return;
        }

        var user = ApiAuthUtil.requireRole(req, res, "FAMILY");
        if (user == null) return;
        var dao = new FamilyNotificationSettingsDao();
        try {
            if (update) {
                String type = req.getContentType();
                if (type == null || !"application/json".equalsIgnoreCase(type.split(";", 2)[0].trim())) {
                    error(res, 415, "UNSUPPORTED_MEDIA_TYPE", "JSON形式で送信してください。");
                    return;
                }
                req.setCharacterEncoding("UTF-8");
                char[] buffer = new char[1025];
                int length = 0;
                var reader = req.getReader();
                while (length < buffer.length) {
                    int count = reader.read(buffer, length, buffer.length - length);
                    if (count == -1) break;
                    length += count;
                }
                if (length == buffer.length) {
                    error(res, 413, "REQUEST_TOO_LARGE", "データが大きすぎます。");
                    return;
                }
                var body = JSON.fromJson(new String(buffer, 0, length), JsonObject.class);
                boolean school = body != null && body.has("schoolNotificationEnabled");
                String key = school ? "schoolNotificationEnabled" : "notificationEnabled";
                var value = body == null ? null : body.get(key);
                if (body == null || body.size() != 1 || value == null || !value.isJsonPrimitive()
                        || !value.getAsJsonPrimitive().isBoolean()) {
                    error(res, 400, "INVALID_INPUT", "通知設定を確認してください。");
                    return;
                }
                boolean enabled = value.getAsBoolean();
                if (!dao.update(user.getId(), school, enabled)) {
                    error(res, 403, "FORBIDDEN", "設定を変更する権限がありません。");
                    return;
                }
                ApiJsonUtil.write(res, 200, Map.of(key, enabled));
                return;
            }

            var settings = dao.get(user.getId());
            if (settings == null) {
                error(res, 403, "FORBIDDEN", "設定を閲覧する権限がありません。");
                return;
            }
            var students = new FamilyStudentDao().findByFamilyUserId(user.getId()).stream()
                    .map(student -> Map.<String, Object>of("studentUserId", student.getStudentUserId(),
                            "studentName", student.getStudentName())).toList();
            ApiJsonUtil.write(res, 200, Map.of("notificationEnabled", settings.answerEnabled(),
                    "schoolNotificationEnabled", settings.schoolEnabled(), "students", students));
        } catch (JsonParseException e) {
            error(res, 400, "INVALID_INPUT", "通知設定を確認してください。");
        } catch (NamingException | SQLException e) {
            req.getServletContext().log("保護者設定の処理に失敗しました。例外=" + e.getClass().getSimpleName());
            error(res, 503, "SERVICE_UNAVAILABLE", "現在利用できません。時間をおいてお試しください。");
        }
    }

    private static void error(HttpServletResponse res, int status, String code, String message)
            throws java.io.IOException {
        ApiJsonUtil.write(res, status, Map.of("error", code, "message", message));
    }
}
