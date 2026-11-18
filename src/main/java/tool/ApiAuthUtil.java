package tool;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;

import javax.naming.NamingException;

import bean.User;
import dao.ApiLoginTokenDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * APIの認証と利用者区分の確認を行う。
 */
public final class ApiAuthUtil {

    private ApiAuthUtil() {
        // インスタンス化しない
    }

    /**
     * アクセストークンと必要な利用者区分を確認する。
     * 失敗時はレスポンスを返し、nullを返す。
     */
    public static User requireRole(
            HttpServletRequest req,
            HttpServletResponse res,
            String... requiredRoles) throws IOException {

        res.setHeader("Cache-Control", "no-store");

        Enumeration<String> headers =
                req.getHeaders("Authorization");

        String authorization = null;

        if (headers != null && headers.hasMoreElements()) {
            authorization = headers.nextElement();

            if (headers.hasMoreElements()) {
                unauthorized(res);
                return null;
            }
        }

        // Cookieやセッションでは認証しない
        if (authorization == null
                || !authorization.matches(
                        "(?i:Bearer) [A-Za-z0-9_-]{43}")) {

            unauthorized(res);
            return null;
        }

        String accessToken =
                authorization.substring("Bearer ".length());

        try {
            User user = new ApiLoginTokenDao()
                    .findByAccessTokenHash(
                            LoginTokenUtil.hashToken(accessToken));

            if (user == null) {
                unauthorized(res);
                return null;
            }

            // DBから取得した最新の利用者区分で判断する
            if (!Arrays.asList(requiredRoles).contains(user.getRole())) {
                ApiJsonUtil.write(
                        res,
                        HttpServletResponse.SC_FORBIDDEN,
                        Map.of(
                                "error", "FORBIDDEN",
                                "message", "この操作は許可されていません。"));
                return null;
            }

            return user;

        } catch (NamingException | SQLException e) {
            // トークンや接続情報は記録しない
            req.getServletContext().log(
                    "API認証処理に失敗しました。例外="
                            + e.getClass().getSimpleName());

            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    Map.of("error", "SERVICE_UNAVAILABLE"));

            return null;
        }
    }

    /**
     * 認証失敗時のレスポンスを返す。
     */
    private static void unauthorized(
            HttpServletResponse res) throws IOException {

        res.setHeader(
                "WWW-Authenticate",
                "Bearer realm=\"AnpiLink\"");

        ApiJsonUtil.write(
                res,
                HttpServletResponse.SC_UNAUTHORIZED,
                Map.of(
                        "error", "UNAUTHORIZED",
                        "message", "再度ログインしてください。"));
    }
}