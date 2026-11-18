package api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;

/**
 * Androidアプリ向けAPIの接続を確認する。
 */
public class PingAction extends Action {

    @Override
    public void execute(
            HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // レスポンス形式を設定する
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.setHeader("Cache-Control", "no-store");

        // GET以外は受け付けない
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");
            res.setStatus(
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED);

            res.getWriter().write(
                    "{\"error\":\"METHOD_NOT_ALLOWED\"}");
            return;
        }

        // 接続確認用の固定データを返す
        res.setStatus(HttpServletResponse.SC_OK);
        res.getWriter().write(
                "{\"status\":\"ok\",\"service\":\"AnpiLink\"}");
    }
}