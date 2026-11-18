package tool;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;

/**
 * FCM HTTP v1 APIでAndroidへ通知を送信する。
 * FCMトークン、認証情報、応答本文はログに出力しない。
 */
public final class FcmSender {

    private static final String CHANNEL_ID =
            "anpilink_notifications";

    private static final int MAX_RESPONSE_BYTES = 65536;

    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT)
            .create();

    private final FcmAccessTokenProvider tokenProvider;
    private final HttpClient httpClient;
    private final URI sendUri;

    public FcmSender()
            throws IOException, GeneralSecurityException {

        tokenProvider = new FcmAccessTokenProvider();

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        sendUri = URI.create(
                "https://fcm.googleapis.com/v1/projects/"
                        + tokenProvider.getProjectId()
                        + "/messages:send");
    }

    /**
     * 指定された端末へ通知を送信する。
     * 成功はFCMでの受付を意味し、端末への到達確認ではない。
     */
    public SendResult send(
            String fcmToken,
            String title,
            String body)
            throws IOException,
            GeneralSecurityException,
            InterruptedException {

        if (fcmToken == null
                || fcmToken.length() > 2048
                || !fcmToken.matches("[\\x21-\\x7E]+")) {

            throw new IllegalArgumentException(
                    "FCMトークンの形式が正しくありません。");
        }

        if (title == null
                || title.isBlank()
                || body == null
                || body.isBlank()) {

            throw new IllegalArgumentException(
                    "通知のタイトルと本文が必要です。");
        }

        JsonObject notification = new JsonObject();
        notification.addProperty("title", title);
        notification.addProperty("body", body);

        JsonObject androidNotification = new JsonObject();
        androidNotification.addProperty(
                "channel_id",
                CHANNEL_ID);
        androidNotification.addProperty(
                "sound",
                "default");

        JsonObject android = new JsonObject();
        android.addProperty("priority", "NORMAL");

        // 古い通知が長期間遅れて届くことを避ける。
        android.addProperty("ttl", "3600s");
        android.add("notification", androidNotification);

        JsonObject message = new JsonObject();
        message.addProperty("token", fcmToken);
        message.add("notification", notification);
        message.add("android", android);

        JsonObject requestBody = new JsonObject();
        requestBody.add("message", message);

        byte[] payload = GSON.toJson(requestBody)
                .getBytes(StandardCharsets.UTF_8);

        // 今回の短い通知用途では送信データ全体を制限する。
        if (payload.length > 4096) {
            throw new IllegalArgumentException(
                    "通知データが大きすぎます。");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(sendUri)
                .timeout(Duration.ofSeconds(30))
                .header(
                        "Authorization",
                        "Bearer " + tokenProvider.getAccessToken())
                .header(
                        "Content-Type",
                        "application/json; charset=UTF-8")
                .POST(
                        HttpRequest.BodyPublishers.ofByteArray(
                                payload))
                .build();

        HttpResponse<InputStream> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream());

        try (InputStream input = response.body()) {
            byte[] bytes = input.readNBytes(
                    MAX_RESPONSE_BYTES + 1);

            if (bytes.length > MAX_RESPONSE_BYTES) {
                throw new IOException(
                        "FCMの応答が大きすぎます。");
            }

            JsonObject responseBody;

            try {
                responseBody = GSON.fromJson(
                        new String(bytes, StandardCharsets.UTF_8),
                        JsonObject.class);

            } catch (JsonParseException e) {
                throw new IOException(
                        "FCMの応答を読み取れませんでした。"
                                + " HTTP=" + response.statusCode());
            }

            if (response.statusCode() == 200) {
                String messageName = getString(
                        responseBody,
                        "name");

                if (messageName == null
                        || messageName.isBlank()) {

                    throw new IOException(
                            "FCMの受付結果を確認できませんでした。");
                }

                return new SendResult(200, null);
            }

            return new SendResult(
                    response.statusCode(),
                    getErrorCode(responseBody));
        }
    }

    /**
     * 応答本文全体ではなく、安全なエラーコードだけ取得する。
     */
    private static String getErrorCode(JsonObject response) {
        if (response == null) {
            return "UNKNOWN";
        }

        JsonElement errorValue = response.get("error");

        if (errorValue == null || !errorValue.isJsonObject()) {
            return "UNKNOWN";
        }

        JsonObject error = errorValue.getAsJsonObject();
        JsonElement details = error.get("details");

        if (details != null && details.isJsonArray()) {
            for (JsonElement item : details.getAsJsonArray()) {
                if (!item.isJsonObject()) {
                    continue;
                }

                JsonObject detail = item.getAsJsonObject();

                if ("type.googleapis.com/google.firebase.fcm.v1.FcmError"
                        .equals(getString(detail, "@type"))) {

                    String code = getString(
                            detail,
                            "errorCode");

                    if (isSafeCode(code)) {
                        return code;
                    }
                }
            }
        }

        String status = getString(error, "status");
        return isSafeCode(status) ? status : "UNKNOWN";
    }

    private static boolean isSafeCode(String code) {
        return code != null
                && code.matches("[A-Z_]{1,64}");
    }

    private static String getString(
            JsonObject object,
            String name) {

        if (object == null) {
            return null;
        }

        JsonElement value = object.get(name);

        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {

            return null;
        }

        return value.getAsString();
    }

    /**
     * 秘密情報を含まない送信結果。
     */
    public static final class SendResult {

        private final int statusCode;
        private final String errorCode;

        public SendResult(
                int statusCode,
                String errorCode) {

            this.statusCode = statusCode;
            this.errorCode = errorCode;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public boolean isAccepted() {
            return statusCode == 200;
        }

        public boolean isUnregistered() {
            return "UNREGISTERED".equals(errorCode);
        }
    }
}