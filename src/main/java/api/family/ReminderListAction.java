package api.family;

import java.sql.SQLException;
import java.util.Map;
import javax.naming.NamingException;
import bean.User;
import dao.SafetyDispatchReminderDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiAuthUtil;
import tool.ApiJsonUtil;

/** 保護者の催促通知一覧。 */
public class ReminderListAction extends Action {
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        res.setHeader("Cache-Control", "no-store");
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");
            ApiJsonUtil.write(res, 405, Map.of("error", "METHOD_NOT_ALLOWED"));
            return;
        }
        User user = ApiAuthUtil.requireRole(req, res, "FAMILY");
        if (user == null) return;
        try {
            var notices = new SafetyDispatchReminderDao().findByFamily(user.getId());
            ApiJsonUtil.write(res, 200, Map.of("notices", notices));
        } catch (NamingException | SQLException e) {
            req.getServletContext().log("保護者催促通知一覧の取得失敗: " + e.getClass().getSimpleName());
            ApiJsonUtil.write(res, 503, Map.of("error", "SERVICE_UNAVAILABLE"));
        }
    }
}
