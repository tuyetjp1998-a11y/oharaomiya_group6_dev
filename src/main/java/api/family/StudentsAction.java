package api.family;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import bean.FamilyStudent;
import bean.User;
import dao.FamilyStudentDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiAuthUtil;
import tool.ApiJsonUtil;

/**
 * 保護者に関連付けられた生徒と最新の安否状況を返す。
 */
public class StudentsAction extends Action {

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

		// アクセストークンで保護者本人を確認する
		User family = ApiAuthUtil.requireRole(
				req, res, "FAMILY");

		if (family == null) {
			return;
		}

		try {
			// リクエスト内の保護者IDは使用しない
			List<FamilyStudent> students = new FamilyStudentDao()
					.findByFamilyUserId(family.getId());

			JsonArray studentArray = new JsonArray();

			for (FamilyStudent student : students) {
				JsonObject item = new JsonObject();

				item.addProperty(
						"studentUserId",
						student.getStudentUserId());

				item.addProperty(
						"studentName",
						student.getStudentName());

				// 未回答の場合もstatus項目を明示する
				if (student.getStatus() == null) {
					item.add("status", JsonNull.INSTANCE);
				} else {
					item.addProperty(
							"status",
							student.getStatus());
				}

				// 未回答や種類が未選択の場合はnullを返す
				item.addProperty(
						"dangerType",
						student.getDangerType());
				studentArray.add(item);
			}

			JsonObject result = new JsonObject();
			result.add("students", studentArray);

			ApiJsonUtil.write(
					res,
					HttpServletResponse.SC_OK,
					result);

		} catch (NamingException | SQLException e) {
			// 生徒情報や接続情報は記録しない
			req.getServletContext().log(
					"API生徒一覧の取得に失敗しました。例外="
							+ e.getClass().getSimpleName());

			ApiJsonUtil.write(
					res,
					HttpServletResponse.SC_SERVICE_UNAVAILABLE,
					Map.of("error", "SERVICE_UNAVAILABLE"));
		}
	}
}