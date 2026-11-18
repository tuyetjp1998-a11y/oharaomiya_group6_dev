package api.student;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import javax.naming.NamingException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;

import bean.User;
import dao.SafetyReportCancellationDao;
import dao.SafetyReportCancellationDao.CancelResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiAuthUtil;
import tool.ApiJsonUtil;

/**
 * Androidアプリから危険回答を取り消す。
 */
public class AnswerCancelAction extends Action {

	private static final int MAX_BODY_CHARS = 1024;

	private static final Gson GSON = new GsonBuilder()
			.setStrictness(Strictness.STRICT)
			.create();

	@Override
	public void execute(
			HttpServletRequest req,
			HttpServletResponse res) throws Exception {

		res.setHeader("Cache-Control", "no-store");

		if (!"POST".equalsIgnoreCase(req.getMethod())) {
			res.setHeader("Allow", "POST");
			writeError(
					res,
					HttpServletResponse.SC_METHOD_NOT_ALLOWED,
					"METHOD_NOT_ALLOWED");
			return;
		}

		// Cookieやリクエストの利用者IDでは認証しない。
		User user = ApiAuthUtil.requireRole(
				req,
				res,
				"STUDENT");

		if (user == null) {
			return;
		}

		String contentType = req.getContentType();

		if (contentType == null
				|| !"application/json".equalsIgnoreCase(
						contentType.split(";", 2)[0].trim())) {

			writeError(
					res,
					HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
					"UNSUPPORTED_MEDIA_TYPE");
			return;
		}

		req.setCharacterEncoding("UTF-8");

		char[] buffer = new char[MAX_BODY_CHARS + 1];
		int length = 0;
		BufferedReader reader = req.getReader();

		while (length < buffer.length) {
			int count = reader.read(
					buffer,
					length,
					buffer.length - length);

			if (count == -1) {
				break;
			}

			length += count;
		}

		if (length > MAX_BODY_CHARS) {
			writeError(res, 413, "REQUEST_TOO_LARGE");
			return;
		}

		JsonObject body;

		try {
			body = GSON.fromJson(
					new String(buffer, 0, length),
					JsonObject.class);

		} catch (JsonParseException e) {
			writeError(
					res,
					HttpServletResponse.SC_BAD_REQUEST,
					"INVALID_JSON");
			return;
		}

		long reportId;

		try {
			reportId = getReportId(body);

		} catch (IllegalArgumentException | ArithmeticException e) {
			writeError(
					res,
					HttpServletResponse.SC_BAD_REQUEST,
					"INVALID_INPUT");
			return;
		}

		try {
			CancelResult result = new SafetyReportCancellationDao().cancel(
					reportId,
					user.getId());

			switch (result) {
			case CANCELLED, ALREADY_CANCELLED -> {
				// 再要求でも取り消し済みなら成功を返す。
				res.setStatus(
						HttpServletResponse.SC_NO_CONTENT);
			}

			case NOT_CANCELLABLE -> {
				writeError(
						res,
						HttpServletResponse.SC_CONFLICT,
						"NOT_CANCELLABLE");
			}

			case NOT_FOUND -> {
				// 他人の回答も存在しない回答と同じ扱いにする。
				writeError(
						res,
						HttpServletResponse.SC_NOT_FOUND,
						"REPORT_NOT_FOUND");
			}
			}

		} catch (NamingException | SQLException e) {

			// 認証情報・位置情報・SQL本文は出力しない。
			String sqlState = "-";
			String constraintName = "-";

			if (e instanceof SQLException sqlException) {
				sqlState = sqlException.getSQLState();

				if (sqlException instanceof org.postgresql.util.PSQLException postgresException) {

					var details = postgresException.getServerErrorMessage();

					if (details != null
							&& details.getConstraint() != null) {
						constraintName = details.getConstraint();
					}
				}
			}

			// SQL本文・回答内容・位置情報は出力しない。
			req.getServletContext().log(
					"API危険回答の取り消しに失敗しました。例外="
							+ e.getClass().getSimpleName()
							+ ", SQLState=" + sqlState
							+ ", constraint=" + constraintName);

			writeError(
					res,
					HttpServletResponse.SC_SERVICE_UNAVAILABLE,
					"SERVICE_UNAVAILABLE");
		}
	}

	/**
	 * 正の整数の回答IDだけを受け付ける。
	 */
	private static long getReportId(JsonObject body) {

		if (body == null) {
			throw new IllegalArgumentException();
		}

		JsonElement value = body.get("reportId");

		if (value == null
				|| !value.isJsonPrimitive()
				|| !value.getAsJsonPrimitive().isNumber()) {

			throw new IllegalArgumentException();
		}

		// 小数やlongの範囲外を拒否する。
		long reportId = value.getAsBigDecimal().longValueExact();

		if (reportId <= 0) {
			throw new IllegalArgumentException();
		}

		return reportId;
	}

	/**
	 * エラー情報をJSON形式で返す。
	 */
	private static void writeError(
			HttpServletResponse res,
			int status,
			String error) throws IOException {

		ApiJsonUtil.write(
				res,
				status,
				Map.of("error", error));
	}
}