package api.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import javax.naming.NamingException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;

import dao.UserDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiJsonUtil;
import tool.PasswordUtil;

/**
 * Androidアプリから保護者アカウントと親子関係の申請を登録する。
 */
public class RegisterAction extends Action {

    private static final int MAX_BODY_BYTES = 8192;
    private static final ZoneId JAPAN_ZONE = ZoneId.of("Asia/Tokyo");

    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT)
            .create();

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        res.setHeader("Cache-Control", "no-store");

        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "POST");
            writeError(res, 405, "METHOD_NOT_ALLOWED",
                    "POSTでアクセスしてください。");
            return;
        }

        String contentType = req.getContentType();
        if (contentType == null || !"application/json".equalsIgnoreCase(
                contentType.split(";", 2)[0].trim())) {
            writeError(res, 415, "UNSUPPORTED_MEDIA_TYPE",
                    "JSON形式で送信してください。");
            return;
        }

        byte[] bytes = req.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (bytes.length > MAX_BODY_BYTES) {
            writeError(res, 413, "REQUEST_TOO_LARGE",
                    "送信データが大きすぎます。");
            return;
        }

        JsonObject body;
        try {
            body = GSON.fromJson(
                    new String(bytes, StandardCharsets.UTF_8),
                    JsonObject.class);
        } catch (JsonParseException e) {
            writeError(res, 400, "INVALID_JSON",
                    "登録情報を読み取れませんでした。");
            return;
        }

        if (body == null) {
            writeError(res, 400, "INVALID_INPUT",
                    "登録情報を入力してください。");
            return;
        }

        String name = getString(body, "name").strip();
        String loginId = getString(body, "loginId").strip();
        String studentLoginId = getString(body, "studentLoginId").strip();

        // パスワードの前後の空白も入力値として扱う。
        String password = getString(body, "password");

        if (name.isBlank() || name.length() > 40) {
            writeError(res, 400, "INVALID_INPUT",
                    "氏名は1～40文字で入力してください。");
            return;
        }

        if (loginId.isBlank() || loginId.length() > 40) {
            writeError(res, 400, "INVALID_INPUT",
                    "ユーザーIDは1～40文字で入力してください。");
            return;
        }

        if (password.isEmpty()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            writeError(res, 400, "INVALID_INPUT",
                    "パスワードはUTF-8で1～72バイトにしてください。");
            return;
        }

        if (studentLoginId.isBlank() || studentLoginId.length() > 40) {
            writeError(res, 400, "INVALID_INPUT",
                    "お子さんの生徒IDは1～40文字で入力してください。");
            return;
        }

        LocalDate today = LocalDate.now(JAPAN_ZONE);
        int schoolYear = today.getMonthValue() >= 4
                ? today.getYear()
                : today.getYear() - 1;

        String passwordHash = PasswordUtil.hash(password);
        boolean registered;
        try {
            registered = new UserDao().registerFamily(
                    loginId, name, passwordHash, studentLoginId, schoolYear);
        } catch (NamingException | SQLException e) {
            // 入力内容やパスワードはログに記録しない。
            req.getServletContext().log(
                    "保護者登録の保存に失敗しました。例外="
                            + e.getClass().getSimpleName());
            writeError(res, 503, "SERVICE_UNAVAILABLE",
                    "現在利用できません。時間をおいてお試しください。");
            return;
        }

        if (!registered) {
            writeError(res, 409, "REGISTRATION_FAILED",
                    "登録できませんでした。ユーザーIDが登録済みでないか、"
                    + "お子さんの生徒IDや所属情報を確認してください。");
            return;
        }

        ApiJsonUtil.write(res, HttpServletResponse.SC_CREATED,
                Map.of("registered", true));
    }

    private static String getString(JsonObject body, String key) {
        JsonElement value = body.get(key);
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            return "";
        }
        return value.getAsString();
    }

    private static void writeError(HttpServletResponse res,
            int status, String code, String message) throws IOException {
        ApiJsonUtil.write(res, status,
                Map.of("error", code, "message", message));
    }
}