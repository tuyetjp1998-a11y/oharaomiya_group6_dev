package api.student;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import javax.naming.NamingException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;

import bean.User;
import dao.StudentSettingsDao;
import dao.StudentSettingsDao.SettingsData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiAuthUtil;
import tool.ApiJsonUtil;

/**
 * 生徒本人の位置情報・通知設定を取得・更新する。
 */
public class SettingsAction extends Action {

    private static final int MAX_BODY_CHARS = 1024;

    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT)
            .create();

    @Override
    public void execute(
            HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        res.setHeader("Cache-Control", "no-store");

        String method = req.getMethod();

        if (!"GET".equalsIgnoreCase(method)
                && !"POST".equalsIgnoreCase(method)) {

            res.setHeader("Allow", "GET, POST");

            writeError(
                    res,
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "METHOD_NOT_ALLOWED",
                    "GETまたはPOSTでアクセスしてください。");
            return;
        }

        // 利用者IDはアクセストークンから取得する。
        User student = ApiAuthUtil.requireRole(
                req, res, "STUDENT");

        if (student == null) {
            return;
        }

        try {
            StudentSettingsDao dao = new StudentSettingsDao();

            if ("GET".equalsIgnoreCase(method)) {
                SettingsData settings =
                        dao.getSettings(student.getId());

                if (settings == null) {
                    writeError(
                            res,
                            HttpServletResponse.SC_FORBIDDEN,
                            "FORBIDDEN",
                            "この操作は許可されていません。");
                    return;
                }

                ApiJsonUtil.write(
                        res,
                        HttpServletResponse.SC_OK,
                        Map.of(
                                "locationEnabled",
                                settings.isLocationEnabled(),
                                "notificationEnabled",
                                settings.isNotificationEnabled()));
                return;
            }

            String contentType = req.getContentType();

            if (contentType == null
                    || !"application/json".equalsIgnoreCase(
                            contentType.split(";", 2)[0].trim())) {

                writeError(
                        res,
                        HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                        "UNSUPPORTED_MEDIA_TYPE",
                        "JSON形式で送信してください。");
                return;
            }

            req.setCharacterEncoding("UTF-8");

            char[] buffer = new char[MAX_BODY_CHARS + 1];
            int length = 0;
            BufferedReader reader = req.getReader();

            while (length < buffer.length) {
                int count = reader.read(
                        buffer,
                        length,
                        buffer.length - length);

                if (count == -1) {
                    break;
                }

                length += count;
            }

            if (length > MAX_BODY_CHARS) {
                writeError(
                        res,
                        413,
                        "REQUEST_TOO_LARGE",
                        "送信データが大きすぎます。");
                return;
            }

            JsonObject body;

            try {
                body = GSON.fromJson(
                        new String(buffer, 0, length),
                        JsonObject.class);

            } catch (JsonParseException e) {
                writeError(
                        res,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "INVALID_JSON",
                        "JSONの形式を確認してください。");
                return;
            }

            // 1回のリクエストでは1項目だけ更新する。
            if (body == null
                    || body.size() != 2
                    || !body.has("setting")
                    || !body.has("enabled")) {

                invalidInput(res);
                return;
            }

            JsonElement settingValue = body.get("setting");
            JsonElement enabledValue = body.get("enabled");

            if (!settingValue.isJsonPrimitive()
                    || !settingValue.getAsJsonPrimitive().isString()
                    || !enabledValue.isJsonPrimitive()
                    || !enabledValue.getAsJsonPrimitive().isBoolean()) {

                invalidInput(res);
                return;
            }

            String setting = settingValue.getAsString();
            boolean enabled = enabledValue.getAsBoolean();

            boolean updated;

            switch (setting) {
                case "locationEnabled":
                    updated = dao.updateLocationEnabled(
                            student.getId(), enabled);
                    break;

                case "notificationEnabled":
                    updated = dao.updateNotificationEnabled(
                            student.getId(), enabled);
                    break;

                default:
                    invalidInput(res);
                    return;
            }

            if (!updated) {
                writeError(
                        res,
                        HttpServletResponse.SC_FORBIDDEN,
                        "FORBIDDEN",
                        "この操作は許可されていません。");
                return;
            }

            // 保存できた項目と値を返す。
            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_OK,
                    Map.of(
                            "setting", setting,
                            "enabled", enabled));

        } catch (NamingException | SQLException e) {
            // 認証情報や接続情報は記録しない。
            req.getServletContext().log(
                    "API生徒設定の処理に失敗しました。例外="
                            + e.getClass().getSimpleName());

            writeError(
                    res,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "SERVICE_UNAVAILABLE",
                    "現在利用できません。時間をおいてお試しください。");
        }
    }

    private static void invalidInput(
            HttpServletResponse res) throws IOException {

        writeError(
                res,
                HttpServletResponse.SC_BAD_REQUEST,
                "INVALID_INPUT",
                "設定内容を確認してください。");
    }

    private static void writeError(
            HttpServletResponse res,
            int status,
            String error,
            String message) throws IOException {

        ApiJsonUtil.write(
                res,
                status,
                Map.of(
                        "error", error,
                        "message", message));
    }
}