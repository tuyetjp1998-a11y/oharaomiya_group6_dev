package api.auth;

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

import dao.ApiLoginTokenDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiJsonUtil;
import tool.LoginTokenUtil;

/**
 * Androidアプリのログアウトを処理する。
 */
public class LogoutAction extends Action {

    private static final int MAX_BODY_CHARS = 1024;

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
                    "METHOD_NOT_ALLOWED");
            return;
        }

        // JSON形式のみ受け付ける
        String contentType = req.getContentType();

        if (contentType == null
                || !"application/json".equalsIgnoreCase(
                        contentType.split(";", 2)[0].trim())) {

            writeError(
                    res,
                    HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    "UNSUPPORTED_MEDIA_TYPE");
            return;
        }

        req.setCharacterEncoding("UTF-8");

        // 読み込む文字数を制限する
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
            writeError(res, 413, "REQUEST_TOO_LARGE");
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
                    "INVALID_JSON");
            return;
        }

        JsonElement value = body == null
                ? null
                : body.get("refreshToken");

        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {

            writeError(
                    res,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_INPUT");
            return;
        }

        String refreshToken = value.getAsString();

        if (!refreshToken.matches("[A-Za-z0-9_-]{43}")) {
            writeError(
                    res,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_INPUT");
            return;
        }

        try {
            new ApiLoginTokenDao().deleteByRefreshTokenHash(
                    LoginTokenUtil.hashToken(refreshToken));

            // 削除済みの場合も成功とし、再送を許可する
            // Web用のCookieやセッションは変更しない
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (NamingException | SQLException e) {
            // トークンや接続情報は記録しない
            req.getServletContext().log(
                    "APIログアウト処理に失敗しました。例外="
                            + e.getClass().getSimpleName());

            writeError(
                    res,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "SERVICE_UNAVAILABLE");
        }
    }

    /**
     * エラー情報をJSON形式で返す。
     */
    private static void writeError(
            HttpServletResponse res,
            int status,
            String error) throws IOException {

        ApiJsonUtil.write(
                res,
                status,
                Map.of("error", error));
    }
}