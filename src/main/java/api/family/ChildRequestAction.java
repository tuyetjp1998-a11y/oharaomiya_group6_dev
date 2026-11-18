package api.family;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import javax.naming.NamingException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import bean.User;
import dao.FamilyStudentRequestDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiAuthUtil;
import tool.ApiJsonUtil;

/**
 * Androidから生徒との関連付け申請を受け付ける。
 */
public class ChildRequestAction extends Action {

    private static final int MAX_BODY_BYTES = 8192;

    @Override
    public void execute(
            HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        res.setHeader("Cache-Control", "no-store");

        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "POST");

            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    Map.of(
                            "error", "METHOD_NOT_ALLOWED",
                            "message", "POSTでアクセスしてください。"));
            return;
        }

        // 申請者はBearerトークンから特定する
        User user = ApiAuthUtil.requireRole(
                req,
                res,
                "FAMILY");

        if (user == null) {
            return;
        }

        String contentType = req.getContentType();

        if (contentType == null
                || !"application/json".equalsIgnoreCase(
                        contentType.split(";", 2)[0].strip())) {

            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    Map.of(
                            "error", "UNSUPPORTED_MEDIA_TYPE",
                            "message", "JSON形式で送信してください。"));
            return;
        }

        byte[] bodyBytes = req.getInputStream()
                .readNBytes(MAX_BODY_BYTES + 1);

        if (bodyBytes.length > MAX_BODY_BYTES) {
            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    Map.of(
                            "error", "PAYLOAD_TOO_LARGE",
                            "message", "送信データが大きすぎます。"));
            return;
        }

        String studentLoginId;

        try (JsonReader reader = new JsonReader(
                new StringReader(
                        new String(
                                bodyBytes,
                                StandardCharsets.UTF_8)))) {

            reader.setStrictness(Strictness.STRICT);

            JsonElement root = JsonParser.parseReader(reader);

            if (!root.isJsonObject()
                    || reader.peek() != JsonToken.END_DOCUMENT) {
                badRequest(res);
                return;
            }

            JsonObject body = root.getAsJsonObject();
            JsonElement value = body.get("studentLoginId");

            // 指定された文字列の生徒IDだけを受け付ける
            if (body.size() != 1
                    || value == null
                    || !value.isJsonPrimitive()
                    || !value.getAsJsonPrimitive().isString()) {
                badRequest(res);
                return;
            }

            studentLoginId = value.getAsString().strip();

        } catch (JsonParseException | IOException e) {
            badRequest(res);
            return;
        }

        if (studentLoginId.isBlank()
                || studentLoginId.length() > 40) {
            badRequest(res);
            return;
        }

        // Webと同じく、日本時間の4月を年度の開始とする
        LocalDate today = LocalDate.now(
                ZoneId.of("Asia/Tokyo"));

        int schoolYear = today.getMonthValue() >= 4
                ? today.getYear()
                : today.getYear() - 1;

        boolean created;

        try {
            created = new FamilyStudentRequestDao().create(
                    user.getId(),
                    studentLoginId,
                    schoolYear);

        } catch (NamingException | SQLException e) {
            req.getServletContext().log(
                    "生徒の関連付け申請処理に失敗しました。例外="
                            + e.getClass().getSimpleName());

            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    Map.of(
                            "error", "SERVICE_UNAVAILABLE",
                            "message",
                            "申請結果を確認できませんでした。"
                                    + "申請履歴を確認してください。"));
            return;
        }

        if (!created) {
            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_CONFLICT,
                    Map.of(
                            "error", "REQUEST_NOT_CREATED",
                            "message",
                            "申請できませんでした。"
                                    + "登録済み・申請中でないか、"
                                    + "生徒IDや所属情報を確認してください。"));
            return;
        }

        ApiJsonUtil.write(
                res,
                HttpServletResponse.SC_CREATED,
                Map.of(
                        "status", "PENDING",
                        "message",
                        "申請を受け付けました。教員の承認をお待ちください。"));
    }

    /**
     * 入力内容が正しくない場合のレスポンス。
     */
    private void badRequest(
            HttpServletResponse res) throws IOException {

        ApiJsonUtil.write(
                res,
                HttpServletResponse.SC_BAD_REQUEST,
                Map.of(
                        "error", "BAD_REQUEST",
                        "message", "生徒IDを1～40文字で入力してください。"));
    }
}