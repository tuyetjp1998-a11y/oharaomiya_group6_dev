package api.student;

import java.sql.SQLException;
import java.util.Map;
import javax.naming.NamingException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import bean.User;
import bean.WorriedType;
import dao.WorriedTypeDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiAuthUtil;
import tool.ApiJsonUtil;

/** 生徒向けに有効な「困っている内容」の一覧を返す。 */
public class WorriedTypeListAction extends Action {
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        res.setHeader("Cache-Control", "no-store");
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");
            ApiJsonUtil.write(res, 405, Map.of("error", "METHOD_NOT_ALLOWED"));
            return;
        }
        User user = ApiAuthUtil.requireRole(req, res, "STUDENT");
        if (user == null) {
            return;
        }
        JsonArray items = new JsonArray();
        try {
            for (WorriedType type : new WorriedTypeDao().findActive()) {
                JsonObject item = new JsonObject();
                item.addProperty("id", type.getId());
                item.addProperty("code", type.getCode());
                item.addProperty("name", type.getName());
                items.add(item);
            }
        } catch (NamingException | SQLException e) {
            req.getServletContext().log("困っている内容一覧取得失敗: " + e.getClass().getSimpleName());
            ApiJsonUtil.write(res, 503, Map.of("error", "SERVICE_UNAVAILABLE"));
            return;
        }
        JsonObject result = new JsonObject();
        result.add("items", items);
        ApiJsonUtil.write(res, 200, result);
    }
}
