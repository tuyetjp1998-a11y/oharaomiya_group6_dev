package api.family;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import bean.FamilyStudentRequest;
import bean.User;
import dao.FamilyStudentRequestDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiAuthUtil;
import tool.ApiJsonUtil;

/**
 * 保護者自身の関連付け申請履歴をJSONで返す。
 */
public class ChildRequestsAction extends Action {

    @Override
    public void execute(
            HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        res.setHeader("Cache-Control", "no-store");

        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");

            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    Map.of(
                            "error", "METHOD_NOT_ALLOWED",
                            "message", "GETでアクセスしてください。"));
            return;
        }

        // 保護者IDはリクエストではなくトークンから取得する
        User user = ApiAuthUtil.requireRole(
                req,
                res,
                "FAMILY");

        if (user == null) {
            return;
        }

        List<Map<String, Object>> items = new ArrayList<>();

        try {
            List<FamilyStudentRequest> requests =
                    new FamilyStudentRequestDao()
                            .findHistoryByFamilyUserId(
                                    user.getId());

            for (FamilyStudentRequest request : requests) {
                Map<String, Object> item =
                        new LinkedHashMap<>();

                item.put(
                        "requestId",
                        request.getId());

                item.put(
                        "studentLoginId",
                        request.getStudentLoginId());

                item.put(
                        "status",
                        request.getStatus());

                // 日付はWebと同じ日本時間の表示形式を使用する
                item.put(
                        "requestedAtText",
                        request.getRequestedAtText());

                items.add(item);
            }

        } catch (NamingException | SQLException e) {
            req.getServletContext().log(
                    "保護者の申請履歴取得に失敗しました。例外="
                            + e.getClass().getSimpleName());

            ApiJsonUtil.write(
                    res,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    Map.of(
                            "error", "SERVICE_UNAVAILABLE",
                            "message",
                            "現在利用できません。時間をおいてお試しください。"));
            return;
        }

        ApiJsonUtil.write(
                res,
                HttpServletResponse.SC_OK,
                Map.of("requests", items));
    }
}