package tool;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jakarta.servlet.http.HttpServletResponse;

/**
 * APIのレスポンスをJSON形式で返す。
 */
public final class ApiJsonUtil {

	private static final Gson GSON = new GsonBuilder()
	        .serializeNulls()
	        .create();
    private ApiJsonUtil() {
        // インスタンス化しない
    }

    /**
     * 指定されたHTTPステータスとJSONデータを返す。
     */
    public static void write(
            HttpServletResponse res,
            int status,
            Object data) throws IOException {

        // 出力前にJSONへ変換する
        String json = GSON.toJson(data);

        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.setHeader("Cache-Control", "no-store");
        res.setHeader("X-Content-Type-Options", "nosniff");

        res.getWriter().write(json);
    }
}