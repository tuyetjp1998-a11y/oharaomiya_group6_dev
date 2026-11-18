package api.push;

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
import dao.AndroidPushSubscriptionDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiAuthUtil;
import tool.ApiJsonUtil;
import tool.LoginTokenUtil;

/**
 * AndroidアプリのFCMトークンを登録する。
 */
public class RegisterAction extends Action {

    private static final int MAX_BODY_CHARS = 8192;

    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT)
            .create();

    @Override
    public void execute(
            HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        res.setHeader("Cache-Control", "no-store");

        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "POST");

            writeError(
                    res,
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "METHOD_NOT_ALLOWED",
                    "POSTで送信してください。");
            return;
        }

        // CookieやセッションではなくBearerで認証する。
        User user = ApiAuthUtil.requireRole(
                req, res, "STUDENT", "FAMILY");

        if (user == null) {
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

        BufferedReader reader = req.getReader();
        char[] buffer = new char[MAX_BODY_CHARS + 1];
        int length = 0;

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
                : body.get("fcmToken");

        String fcmToken = null;

        if (value != null
                && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isString()) {

            fcmToken = value.getAsString();
        }

        // トークンを自動でトリムしない。
        if (fcmToken == null
                || fcmToken.length() > 2048
                || !fcmToken.matches("[\\x21-\\x7E]+")) {

            writeError(
                    res,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_INPUT",
                    "通知登録情報を確認してください。");
            return;
        }

        // AuthorizationヘッダーはApiAuthUtilで確認済み。
        String accessToken = req.getHeader("Authorization")
                .substring("Bearer ".length());

        try {
            boolean saved = new AndroidPushSubscriptionDao()
                    .save(
                            LoginTokenUtil.hashToken(accessToken),
                            fcmToken);

            if (!saved) {
                res.setHeader(
                        "WWW-Authenticate",
                        "Bearer realm=\"AnpiLink\"");

                writeError(
                        res,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "UNAUTHORIZED",
                        "再度ログインしてください。");
                return;
            }

            // 本文を返さず、登録成功を通知する。
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (NamingException | SQLException e) {
            // FCMトークンや認証トークンは記録しない。
        	String sqlState = e instanceof SQLException
        	        ? ((SQLException) e).getSQLState()
        	        : "-";

        	req.getServletContext().log(
        	        "Android通知登録に失敗しました。例外="
        	                + e.getClass().getSimpleName()
        	                + ", SQLState=" + sqlState);

            writeError(
                    res,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "SERVICE_UNAVAILABLE",
                    "現在通知を登録できません。"
                            + "時間をおいて再度お試しください。");
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