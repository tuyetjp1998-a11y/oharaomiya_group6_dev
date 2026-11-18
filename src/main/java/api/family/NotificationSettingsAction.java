package api.family;

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
import dao.FamilyNotificationSettingDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiAuthUtil;
import tool.ApiJsonUtil;

/**
 * Androidアプリから保護者の安否回答通知設定を更新する。
 */
public class NotificationSettingsAction extends Action {

    private static final int MAX_BODY_CHARS = 1024;

    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT)
            .create();

    @Override
    public void execute(
            HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        res.setHeader("Cache-Control", "no-store");

        // 設定変更はPOSTで受け付ける
        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "POST");

            writeError(
                    res,
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "METHOD_NOT_ALLOWED",
                    "POSTで送信してください。");
            return;
        }

        // 更新対象はアクセストークンから特定する
        User family = ApiAuthUtil.requireRole(
                req, res, "FAMILY");

        if (family == null) {
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

        JsonElement value = body == null
                ? null
                : body.get("notificationEnabled");

        // 文字列の"true"や数値は受け付けない
        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isBoolean()) {

            writeError(
                    res,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_INPUT",
                    "通知設定を確認してください。");
            return;
        }

        boolean notificationEnabled = value.getAsBoolean();

        try {
            boolean updated =
                    new FamilyNotificationSettingDao().update(
                            family.getId(),
                            notificationEnabled);

            // 認証後にアカウントが無効化された場合など
            if (!updated) {
                writeError(
                        res,
                        HttpServletResponse.SC_FORBIDDEN,
                        "FORBIDDEN",
                        "設定を変更する権限がありません。");
                return;
            }

            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_OK,
                    Map.of(
                            "notificationEnabled",
                            notificationEnabled));

        } catch (NamingException | SQLException e) {
            // 認証トークンや接続情報は記録しない
            req.getServletContext().log(
                    "保護者通知設定の更新に失敗しました。例外="
                            + e.getClass().getSimpleName());

            writeError(
                    res,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "SERVICE_UNAVAILABLE",
                    "設定を保存できませんでした。"
                            + "時間をおいてお試しください。");
        }
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