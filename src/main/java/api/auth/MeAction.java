package api.auth;

import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map;

import javax.naming.NamingException;

import com.google.gson.JsonObject;

import bean.User;
import dao.ApiLoginTokenDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiJsonUtil;
import tool.LoginTokenUtil;

/**
 * アクセストークンで認証し、利用者情報を返す。
 */
public class MeAction extends Action {

    @Override
    public void execute(
            HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        res.setHeader("Cache-Control", "no-store");

        // GET以外は受け付けない
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");

            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    Map.of("error", "METHOD_NOT_ALLOWED"));
            return;
        }

        // Authorizationヘッダーが1つだけあることを確認する
        Enumeration<String> headers =
                req.getHeaders("Authorization");

        String authorization = null;

        if (headers != null && headers.hasMoreElements()) {
            authorization = headers.nextElement();

            if (headers.hasMoreElements()) {
                unauthorized(res);
                return;
            }
        }

        // Cookie、セッション、URLパラメーターでは認証しない
        if (authorization == null
                || !authorization.matches(
                        "(?i:Bearer) [A-Za-z0-9_-]{43}")) {

            unauthorized(res);
            return;
        }

        String accessToken =
                authorization.substring("Bearer ".length());

        try {
            User user = new ApiLoginTokenDao()
                    .findByAccessTokenHash(
                            LoginTokenUtil.hashToken(accessToken));

            if (user == null) {
                unauthorized(res);
                return;
            }

            // 必要な情報だけを返す
            JsonObject userJson = new JsonObject();
            userJson.addProperty("id", user.getId());
            userJson.addProperty("name", user.getName());
            userJson.addProperty("role", user.getRole());

            JsonObject result = new JsonObject();
            result.add("user", userJson);

            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_OK,
                    result);

        } catch (NamingException | SQLException e) {
            // トークンや接続情報は記録しない
            req.getServletContext().log(
                    "API認証処理に失敗しました。例外="
                            + e.getClass().getSimpleName());

            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    Map.of("error", "SERVICE_UNAVAILABLE"));
        }
    }

    /**
     * 認証失敗時は共通のレスポンスを返す。
     */
    private static void unauthorized(
            HttpServletResponse res) throws java.io.IOException {

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