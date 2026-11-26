package api.student;

import java.sql.SQLException;
import java.util.Map;
import javax.naming.NamingException;
import bean.User;
import dao.SafetyDispatchDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiAuthUtil;
import tool.ApiJsonUtil;

/** 本人宛ての通知を既読にする。 */
public class DispatchReadAction extends Action {
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        res.setHeader("Cache-Control", "no-store");
        req.setCharacterEncoding("UTF-8");
        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "POST");
            ApiJsonUtil.write(res, 405, Map.of("error", "METHOD_NOT_ALLOWED"));
            return;
        }
        User user = ApiAuthUtil.requireRole(req, res, "STUDENT");
        if (user == null) return;

        String[] values = req.getParameterValues("dispatchId");
        long id;
        try {
            if (values == null || values.length != 1
                    || !values[0].matches("[1-9][0-9]{0,18}")) {
                throw new IllegalArgumentException();
            }
            id = Long.parseLong(values[0]);
        } catch (IllegalArgumentException e) {
            ApiJsonUtil.write(res, 400, Map.of("error", "INVALID_INPUT"));
            return;
        }
        try {
            if (!new SafetyDispatchDao().markRead(user.getId(), id, "STUDENT")) {
                ApiJsonUtil.write(res, 404, Map.of("error", "NOT_FOUND"));
                return;
            }
            ApiJsonUtil.write(res, 200, Map.of("read", true));
        } catch (NamingException | SQLException e) {
            req.getServletContext().log("通知既読登録失敗: " + e.getClass().getSimpleName());
            ApiJsonUtil.write(res, 503, Map.of("error", "SERVICE_UNAVAILABLE"));
        }
    }
}
