package api.student;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Map;
import javax.naming.NamingException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import bean.User;
import dao.SafetyDispatchDao;
import dao.SafetyDispatchDao.DispatchNotice;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiAuthUtil;
import tool.ApiJsonUtil;

/** 生徒本人宛ての学校からの連絡を取得する。 */
public class DispatchListAction extends Action {
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

        JsonArray notices = new JsonArray();
        try {
            for (DispatchNotice notice : new SafetyDispatchDao().findByStudent(user.getId())) {
                JsonObject item = new JsonObject();
                item.addProperty("dispatchId", notice.dispatchId());
                item.addProperty("title", notice.title());
                item.addProperty("message", notice.message());
                item.addProperty("dispatchedAt", formatTime(notice.dispatchedAt()));
                item.addProperty("deadlineAt", formatTime(notice.deadlineAt()));
                item.addProperty("readAt", formatTime(notice.readAt()));
                item.addProperty("answered", notice.answered());
                notices.add(item);
            }
        } catch (NamingException | SQLException e) {
            req.getServletContext().log("配信一覧取得失敗: " + e.getClass().getSimpleName());
            ApiJsonUtil.write(res, 503, Map.of("error", "SERVICE_UNAVAILABLE"));
            return;
        }

        JsonObject result = new JsonObject();
        result.add("notices", notices);
        ApiJsonUtil.write(res, 200, result);
    }

    private static String formatTime(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
