package api.auth;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Map;

import javax.naming.NamingException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;

import bean.User;
import dao.ApiLoginTokenDao;
import dao.UserDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiJsonUtil;
import tool.LoginTokenUtil;
import tool.PasswordUtil;

/**
 * Androidアプリのログインを処理する。
 */
public class LoginAction extends Action {

    private static final int MAX_BODY_CHARS = 8192;

    private static final int ACCESS_VALID_MINUTES = 15;
    private static final int REFRESH_VALID_DAYS = 180;

    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT)
            .create();

    @Override
    public void execute(
            HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        res.setHeader("Cache-Control", "no-store");

        // POST以外は受け付けない
        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "POST");
            writeError(
                    res,
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "METHOD_NOT_ALLOWED",
                    "POSTで送信してください。");
            return;
        }

        // JSON形式のリクエストのみ受け付ける
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

        // 読み込む文字数を制限する
        char[] buffer = new char[MAX_BODY_CHARS + 1];
        int length = 0;

        java.io.BufferedReader reader = req.getReader();

        // 上限を超えるか、本文の末尾に達するまで読み込む
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

        String loginId = getString(body, "loginId");
        String password = getString(body, "password");

        // IDやパスワードを自動でトリムしない
        if (loginId == null
                || loginId.isBlank()
                || loginId.length() > 100
                || password == null
                || password.isEmpty()) {

            writeError(
                    res,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_INPUT",
                    "ログインIDとパスワードを確認してください。");
            return;
        }

        try {
            UserDao.LoginData loginData =
                    new UserDao().findByLoginId(loginId);

            User user = loginData == null
                    ? null
                    : loginData.getUser();

            boolean passwordCorrect = loginData != null
                    && PasswordUtil.verify(
                            password,
                            loginData.getPasswordHash());

            // 利用者区分はリクエストではなくDBの値で判断する
            if (!passwordCorrect
                    || user == null
                    || !user.isActive()
                    || !("STUDENT".equals(user.getRole())
                            || "FAMILY".equals(user.getRole()))) {

                writeError(
                        res,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "LOGIN_FAILED",
                        "ログインIDまたはパスワードが正しくありません。");
                return;
            }

            // 用途ごとに異なるトークンを生成する
            String accessToken = LoginTokenUtil.createToken();
            String refreshToken = LoginTokenUtil.createToken();

            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime accessExpiresAt =
                    now.plusMinutes(ACCESS_VALID_MINUTES);
            OffsetDateTime expiresAt =
                    now.plusDays(REFRESH_VALID_DAYS);

            Long tokenId = new ApiLoginTokenDao().saveAndReturnId(
                    user.getId(),
                    LoginTokenUtil.hashToken(accessToken),
                    LoginTokenUtil.hashToken(refreshToken),
                    accessExpiresAt,
                    expiresAt);

            // 保存時にアカウントが無効化された場合など
            if (tokenId == null) {
                writeError(
                        res,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "LOGIN_FAILED",
                        "ログインIDまたはパスワードが正しくありません。");
                return;
            }

            // レスポンスに必要な利用者情報だけを設定する
            JsonObject userJson = new JsonObject();
            userJson.addProperty("id", user.getId());
            userJson.addProperty("name", user.getName());
            userJson.addProperty("role", user.getRole());

            JsonObject result = new JsonObject();
            result.addProperty("tokenType", "Bearer");
            result.addProperty("accessToken", accessToken);
            result.addProperty(
                    "accessExpiresAt",
                    accessExpiresAt.toString());
            result.addProperty("refreshToken", refreshToken);
            result.addProperty(
                    "refreshExpiresAt",
                    expiresAt.toString());
            result.add("user", userJson);

            // Web用のCookieやセッションは作成しない
            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_OK,
                    result);

        } catch (NamingException | SQLException e) {
            // パスワード、トークン、接続情報は記録しない
            req.getServletContext().log(
                    "APIログイン処理に失敗しました。例外="
                            + e.getClass().getSimpleName());

            writeError(
                    res,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "SERVICE_UNAVAILABLE",
                    "現在ログインできません。時間をおいて再度お試しください。");
        }
    }

    /**
     * JSONオブジェクトから文字列だけを取得する。
     */
    private static String getString(
            JsonObject body,
            String key) {

        if (body == null) {
            return null;
        }

        JsonElement value = body.get(key);

        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            return null;
        }

        return value.getAsString();
    }

    /**
     * エラー情報をJSON形式で返す。
     */
    private static void writeError(
            HttpServletResponse res,
            int status,
            String error,
            String message) throws java.io.IOException {

        ApiJsonUtil.write(
                res,
                status,
                Map.of(
                        "error", error,
                        "message", message));
    }
}