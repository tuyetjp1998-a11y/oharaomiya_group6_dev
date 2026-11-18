package tool;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;

/**
 * サービスアカウントの秘密鍵でGoogleのアクセストークンを取得する。
 * 秘密鍵やトークンはログに出力しない。
 */
public final class FcmAccessTokenProvider {

    private static final String TOKEN_URL =
            "https://oauth2.googleapis.com/token";

    private static final String FCM_SCOPE =
            "https://www.googleapis.com/auth/firebase.messaging";

    private static final int MAX_JSON_BYTES = 65536;

    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT)
            .create();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private final String projectId;
    private final String clientEmail;
    private final PrivateKey privateKey;

    private String cachedToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public FcmAccessTokenProvider()
            throws IOException, GeneralSecurityException {

        String credentialsPath =
                System.getenv("GOOGLE_APPLICATION_CREDENTIALS");

        if (credentialsPath == null
                || credentialsPath.isBlank()) {

            throw new IOException(
                    "GOOGLE_APPLICATION_CREDENTIALSが未設定です。");
        }

        JsonObject credentials;

        try (InputStream input = Files.newInputStream(
                Path.of(credentialsPath))) {

            byte[] bytes = input.readNBytes(MAX_JSON_BYTES + 1);

            if (bytes.length > MAX_JSON_BYTES) {
                throw new IOException(
                        "Firebase認証ファイルが大きすぎます。");
            }

            credentials = parseJson(bytes);
        }

        if (!"service_account".equals(
                requireString(credentials, "type"))) {

            throw new IOException(
                    "サービスアカウントの認証ファイルではありません。");
        }

        projectId = requireString(credentials, "project_id");
        clientEmail = requireString(credentials, "client_email");

        if (!projectId.matches("[a-z][a-z0-9-]{4,61}[a-z0-9]")) {
            throw new IOException(
                    "FirebaseプロジェクトIDの形式が正しくありません。");
        }

        String pem = requireString(credentials, "private_key");

        if (!pem.contains("-----BEGIN PRIVATE KEY-----")
                || !pem.contains("-----END PRIVATE KEY-----")) {

            throw new IOException(
                    "Firebase秘密鍵の形式が正しくありません。");
        }

        String encodedKey = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException e) {
            throw new IOException(
                    "Firebase秘密鍵を読み取れませんでした。");
        }

        privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(
                        new PKCS8EncodedKeySpec(keyBytes));
    }

    public String getProjectId() {
        return projectId;
    }

    /**
     * 有効なトークンは再利用し、有効期限の60秒前から更新する。
     */
    public synchronized String getAccessToken()
            throws IOException,
            GeneralSecurityException,
            InterruptedException {

        if (cachedToken != null
                && Instant.now().plusSeconds(60)
                        .isBefore(tokenExpiresAt)) {

            return cachedToken;
        }

        Instant requestStartedAt = Instant.now();

        JsonObject header = new JsonObject();
        header.addProperty("alg", "RS256");
        header.addProperty("typ", "JWT");

        JsonObject claims = new JsonObject();
        claims.addProperty("iss", clientEmail);
        claims.addProperty("scope", FCM_SCOPE);
        claims.addProperty("aud", TOKEN_URL);
        claims.addProperty(
                "iat",
                requestStartedAt.minusSeconds(30).getEpochSecond());
        claims.addProperty(
                "exp",
                requestStartedAt.plusSeconds(3500).getEpochSecond());

        String unsignedJwt =
                encode(GSON.toJson(header)
                        .getBytes(StandardCharsets.UTF_8))
                + "."
                + encode(GSON.toJson(claims)
                        .getBytes(StandardCharsets.UTF_8));

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(
                unsignedJwt.getBytes(StandardCharsets.US_ASCII));

        String assertion =
                unsignedJwt + "." + encode(signer.sign());

        String form =
                "grant_type="
                + URLEncoder.encode(
                        "urn:ietf:params:oauth:grant-type:jwt-bearer",
                        StandardCharsets.UTF_8)
                + "&assertion="
                + URLEncoder.encode(
                        assertion,
                        StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(30))
                .header(
                        "Content-Type",
                        "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        form,
                        StandardCharsets.UTF_8))
                .build();

        HttpResponse<InputStream> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream());

        try (InputStream input = response.body()) {
            if (response.statusCode() != 200) {
                throw new IOException(
                        "Google認証失敗: HTTP "
                                + response.statusCode());
            }

            byte[] bytes = input.readNBytes(MAX_JSON_BYTES + 1);

            if (bytes.length > MAX_JSON_BYTES) {
                throw new IOException(
                        "Google認証の応答が大きすぎます。");
            }

            JsonObject body = parseJson(bytes);

            String token = requireString(body, "access_token");
            String tokenType = requireString(body, "token_type");

            if (!"Bearer".equalsIgnoreCase(tokenType)
                    || !token.matches("[\\x21-\\x7E]+")) {

                throw new IOException(
                        "Google認証の応答形式が正しくありません。");
            }

            long expiresIn;

            try {
                expiresIn = body.get("expires_in").getAsLong();
            } catch (RuntimeException e) {
                throw new IOException(
                        "Google認証の有効期限を取得できませんでした。");
            }

            if (expiresIn <= 60 || expiresIn > 86400) {
                throw new IOException(
                        "Google認証の有効期限が正しくありません。");
            }

            cachedToken = token;
            tokenExpiresAt = requestStartedAt.plusSeconds(
                    Math.min(expiresIn, 3600));

            return cachedToken;
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private static JsonObject parseJson(byte[] bytes)
            throws IOException {

        try {
            JsonObject body = GSON.fromJson(
                    new String(bytes, StandardCharsets.UTF_8),
                    JsonObject.class);

            if (body == null) {
                throw new IOException(
                        "認証情報のJSONが空です。");
            }

            return body;

        } catch (JsonParseException e) {
            throw new IOException(
                    "認証情報のJSONを読み取れませんでした。");
        }
    }

    private static String requireString(
            JsonObject body,
            String name) throws IOException {

        JsonElement value = body.get(name);

        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()
                || value.getAsString().isBlank()) {

            throw new IOException(
                    "認証情報の必須項目が不足しています: " + name);
        }

        return value.getAsString();
    }
}