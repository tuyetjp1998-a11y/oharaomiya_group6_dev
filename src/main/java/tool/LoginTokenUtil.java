package tool;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ログイン維持用トークンとCookieを管理する。
 */
public final class LoginTokenUtil {

    public static final int VALID_DAYS = 180;

    private static final String COOKIE_NAME =
            "anpilink_login_token";

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private LoginTokenUtil() {
        // インスタンス化しない
    }

    /**
     * 推測困難なトークンを生成する。
     */
    public static String createToken() {

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    /**
     * DB保存用のSHA-256ハッシュ値を生成する。
     */
    public static String hashToken(String token) {

        try {
            byte[] hash = MessageDigest
                    .getInstance("SHA-256")
                    .digest(token.getBytes(
                            StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256を利用できません。", e);
        }
    }

    /**
     * Cookieから形式が正しいトークンを取得する。
     * 同名Cookieが複数ある場合は使用しない。
     */
    public static String readToken(
            HttpServletRequest req) {

        Cookie[] cookies = req.getCookies();

        if (cookies == null) {
            return null;
        }

        String token = null;
        boolean found = false;

        for (Cookie cookie : cookies) {
            if (!COOKIE_NAME.equals(cookie.getName())) {
                continue;
            }

            if (found) {
                return null;
            }

            found = true;
            token = cookie.getValue();
        }

        if (token == null
                || !token.matches("[A-Za-z0-9_-]{43}")) {
            return null;
        }

        return token;
    }

    /**
     * 有効期限までトークンをCookieに保存する。
     */
    public static void writeCookie(
            HttpServletRequest req,
            HttpServletResponse res,
            String token,
            OffsetDateTime expiresAt) {

        long remainingSeconds = Duration.between(
                OffsetDateTime.now(),
                expiresAt).getSeconds();

        if (remainingSeconds <= 0) {
            clearCookie(req, res);
            return;
        }

        Cookie cookie = createCookie(req, token);

        cookie.setMaxAge((int) Math.min(
                remainingSeconds,
                Duration.ofDays(VALID_DAYS).getSeconds()));

        res.addCookie(cookie);
    }

    /**
     * この端末のログイン維持用Cookieを削除する。
     */
    public static void clearCookie(
            HttpServletRequest req,
            HttpServletResponse res) {

        Cookie cookie = createCookie(req, "");
        cookie.setMaxAge(0);

        res.addCookie(cookie);
    }

    /**
     * 共通のCookie属性を設定する。
     */
    private static Cookie createCookie(
            HttpServletRequest req,
            String value) {

        Cookie cookie = new Cookie(COOKIE_NAME, value);

        String contextPath = req.getContextPath();

        cookie.setPath(
                contextPath.isEmpty() ? "/" : contextPath + "/");

        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");

        // HTTPを許可するのはローカル開発環境のみ
        String serverName = req.getServerName();

        boolean localDevelopment =
                "localhost".equalsIgnoreCase(serverName)
                || "127.0.0.1".equals(serverName)
                || "::1".equals(serverName)
                || "[::1]".equals(serverName);

        // 本番環境では常にHTTPS専用にする
        cookie.setSecure(req.isSecure() || !localDevelopment);

        return cookie;
    }
}