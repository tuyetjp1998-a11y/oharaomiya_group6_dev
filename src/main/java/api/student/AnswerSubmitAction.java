package api.student;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;

import bean.SafetyReport;
import bean.User;
import dao.AndroidPushSubscriptionDao;
import dao.AndroidPushSubscriptionDao.FamilyNotificationTarget;
import dao.SafetyReportDao;
import dao.WorriedTypeDao;
import dao.SafetyReportDao.SubmissionResult;
import dao.SafetyReportDao.SubmissionStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tool.Action;
import tool.ApiAuthUtil;
import tool.ApiJsonUtil;
import tool.FcmSender;

/**
 * Androidアプリから安否回答と位置情報を登録する。
 */
public class AnswerSubmitAction extends Action {

	private static final Logger LOGGER = Logger.getLogger(AnswerSubmitAction.class.getName());

	private static final int MAX_BODY_CHARS = 8192;

	private static final Gson GSON = new GsonBuilder()
			.setStrictness(Strictness.STRICT)
			.create();

	private static final Set<String> DANGER_TYPES = Set.of(
			"ACCIDENT",
			"EARTHQUAKE",
			"FIRE",
			"LANDSLIDE",
			"STALKING");

	@Override
	public void execute(
			HttpServletRequest req,
			HttpServletResponse res) throws Exception {

		res.setHeader("Cache-Control", "no-store");

		// POST以外は受け付けない
		if (!"POST".equalsIgnoreCase(req.getMethod())) {
			res.setHeader("Allow", "POST");
			writeError(res, 405, "METHOD_NOT_ALLOWED");
			return;
		}

		// アクセストークンで生徒本人を確認する
		User user = ApiAuthUtil.requireRole(req, res, "STUDENT");

		if (user == null) {
			return;
		}

		String contentType = req.getContentType();

		if (contentType == null
				|| !"application/json".equalsIgnoreCase(
						contentType.split(";", 2)[0].trim())) {

			writeError(res, 415, "UNSUPPORTED_MEDIA_TYPE");
			return;
		}

		req.setCharacterEncoding("UTF-8");

		// 読み込む文字数を制限する
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
			writeError(res, 400, "INVALID_JSON");
			return;
		}

		if (body == null) {
			writeError(res, 400, "INVALID_INPUT");
			return;
		}

		SafetyReport report = new SafetyReport();
		UUID requestId = null;
		Long dispatchId;

		try {
			// 旧アプリはrequestIdなしでも引き続き受け付ける。
			String requestIdText = getString(body, "requestId");

			if (requestIdText != null) {
				if (!requestIdText.matches(
						"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-"
								+ "[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-"
								+ "[0-9a-fA-F]{12}")) {

					throw new IllegalArgumentException();
				}

				requestId = UUID.fromString(requestIdText);
			}

			// 配信への回答では再送時にも同じ配信IDとリクエストIDを使用する。
			dispatchId = getDispatchId(body);
			if (dispatchId != null && requestId == null) {
				throw new IllegalArgumentException();
			}

			String status = getString(body, "status");
			String dangerType = getString(body, "dangerType");
			String comment = getString(body, "comment");

			// PostgreSQLのtextに保存できない文字を拒否する。
			if (comment != null && comment.indexOf('\0') >= 0) {
				throw new IllegalArgumentException();
			}

			// コメントの前後の空白を除き、空の場合はnullにする。
			if (comment != null) {
				comment = comment.strip();

				if (comment.isBlank()) {
					comment = null;
				}
			}

			if ("SAFE".equals(status)) {

				if (dangerType != null && !dangerType.isBlank()) {
					throw new IllegalArgumentException();
				}

				if (comment != null) {
					throw new IllegalArgumentException();
				}

				dangerType = null;

			} else if ("DANGER".equals(status)) {

				// ホームの危険ボタンは種類未指定で送信する。
				// 旧画面の種類付き回答も引き続き受け付ける。
				if (dangerType != null
						&& !DANGER_TYPES.contains(dangerType)) {

					throw new IllegalArgumentException();
				}

				if (comment != null) {
					throw new IllegalArgumentException();
				}

			} else if ("WORRIED".equals(status)) {

				if (dangerType != null
						&& (dangerType.isBlank() || dangerType.length() > 50
								|| dangerType.indexOf('\0') >= 0)) {

					throw new IllegalArgumentException();
				}

				// 項目の選択、またはコメントの入力が必要。
				if (dangerType == null && comment == null) {
					throw new IllegalArgumentException();
				}

			} else {
				throw new IllegalArgumentException();
			}

			report.setStatus(status);
			report.setDangerType(dangerType);
			report.setComment(comment);

			// 位置情報がなくても回答を受け付ける。
			applyLocation(body, report);

		} catch (IllegalArgumentException
				| java.time.format.DateTimeParseException e) {

			writeError(res, 400, "INVALID_INPUT");
			return;
		}
		report.setUserId(user.getId());

		boolean newlyCreated;

		try {
			// 論理削除済みの項目も、オフライン回答の再送に備えて許可する。
			if ("WORRIED".equals(report.getStatus())
					&& report.getDangerType() != null
					&& !new WorriedTypeDao().existsByCode(report.getDangerType())) {
				writeError(res, 400, "INVALID_INPUT");
				return;
			}

			SafetyReportDao reportDao = new SafetyReportDao();

			if (requestId == null) {
				// 旧アプリとの互換性を維持する。
				boolean saved = reportDao.save(report);

				if (!saved) {
					writeError(res, 500, "SAVE_FAILED");
					return;
				}

				newlyCreated = true;

			} else {
				String payloadHash = createPayloadHash(report);

				SubmissionResult submission = reportDao.saveOnce(
						report,
						requestId,
						payloadHash,
						dispatchId);

				SubmissionStatus submissionStatus = submission.getStatus();

				if (submissionStatus == SubmissionStatus.CONFLICT) {
					writeError(
							res,
							409,
							"REQUEST_ID_CONFLICT");
					return;
				}

				if (submissionStatus == SubmissionStatus.GONE) {
					writeError(
							res,
							410,
							"REPORT_GONE");
					return;
				}

				if (submissionStatus == SubmissionStatus.FORBIDDEN) {
					writeError(
							res,
							403,
							"FORBIDDEN");
					return;
				}

				newlyCreated = submissionStatus == SubmissionStatus.CREATED;

				// 再要求では最初に保存した回答を使用する。
				report = submission.getReport();
			}

		} catch (NamingException | SQLException e) {
		    // 回答内容・位置情報・接続情報はログに出力しない。
		    if (e instanceof SQLException sqlException) {
		        req.getServletContext().log(
		                "API安否回答の保存失敗: SQLState="
		                        + sqlException.getSQLState()
		                        + ", ErrorCode="
		                        + sqlException.getErrorCode());
		    } else {
		        req.getServletContext().log(
		                "API安否回答の保存失敗: 例外="
		                        + e.getClass().getSimpleName());
		    }

		    writeError(res, 503, "SERVICE_UNAVAILABLE");
		    return;
		}

		JsonObject result = new JsonObject();
		result.addProperty("reportId", report.getId());
		if (dispatchId != null) {
			result.addProperty("dispatchId", dispatchId);
		}
		result.addProperty("status", report.getStatus());
		result.addProperty("dangerType", report.getDangerType());
		result.addProperty("comment", report.getComment());
		result.addProperty(
				"reportedAt",
				report.getReportedAt().toString());
		result.addProperty(
				"latitude",
				report.getLatitude());

		result.addProperty(
				"longitude",
				report.getLongitude());

		result.addProperty(
				"locationAccuracy",
				report.getLocationAccuracy());

		result.addProperty(
				"locationAcquiredAt",
				report.getLocationAcquiredAt() == null
						? null
						: report.getLocationAcquiredAt().toString());
		ApiJsonUtil.write(
				res,
				HttpServletResponse.SC_CREATED,
				result);
		// 回答結果を先に返し、FCM通信で完了画面の表示を待たせない。
		// 危険回答の取り消し操作へ進めるようにする。
		res.flushBuffer();

		// 同じ回答の再要求では通知を重複送信しない。
		if (newlyCreated) {
			notifyFamilies(user.getId());
		}
		;
	}

	/**
	 * 検証・正規化後の回答内容から比較用ハッシュを作成する。
	 * requestIdや認証トークンは含めない。
	 */
	private static String createPayloadHash(
			SafetyReport report)
			throws NoSuchAlgorithmException {

		JsonObject payload = new JsonObject();

		payload.addProperty(
				"status",
				report.getStatus());

		payload.addProperty(
				"dangerType",
				report.getDangerType());

		payload.addProperty(
				"comment",
				report.getComment());

		payload.addProperty(
				"latitude",
				report.getLatitude());

		payload.addProperty(
				"longitude",
				report.getLongitude());

		payload.addProperty(
				"locationAccuracy",
				report.getLocationAccuracy());

		// 同じ時刻はタイムゾーン表記が異なっても同じ値にする。
		payload.addProperty(
				"locationAcquiredAt",
				report.getLocationAcquiredAt() == null
						? null
						: report.getLocationAcquiredAt()
								.toInstant()
								.toString());

		byte[] bytes = payload.toString()
				.getBytes(StandardCharsets.UTF_8);

		byte[] digest = MessageDigest
				.getInstance("SHA-256")
				.digest(bytes);

		return HexFormat.of().formatHex(digest);
	}

	/**
	 * 任意の文字列項目を取得する。
	 */
	private static String getString(
			JsonObject body,
			String key) {

		JsonElement value = body.get(key);

		if (value == null || value.isJsonNull()) {
			return null;
		}

		if (!value.isJsonPrimitive()
				|| !value.getAsJsonPrimitive().isString()) {

			throw new IllegalArgumentException();
		}

		return value.getAsString();
	}

	/**
	 * 任意の配信IDを正の整数として取得する。
	 */
	private static Long getDispatchId(JsonObject body) {
		JsonElement value = body.get("dispatchId");
		if (value == null || value.isJsonNull()) {
			return null;
		}
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
				|| !value.getAsString().matches("[1-9][0-9]{0,18}")) {
			throw new IllegalArgumentException();
		}
		return Long.parseLong(value.getAsString());
	}

	/** 数値項目を取得する。 */
	private static double getNumber(
			JsonObject body,
			String key) {

		JsonElement value = body.get(key);

		if (value == null
				|| !value.isJsonPrimitive()
				|| !value.getAsJsonPrimitive().isNumber()) {

			throw new IllegalArgumentException();
		}

		double number = value.getAsDouble();

		if (!Double.isFinite(number)) {
			throw new IllegalArgumentException();
		}

		return number;
	}

	/**
	 * 位置情報は4項目すべてがある場合だけ登録する。
	 */
	private static void applyLocation(
			JsonObject body,
			SafetyReport report) {

		String[] keys = {
				"latitude",
				"longitude",
				"locationAccuracy",
				"locationAcquiredAt"
		};

		int present = 0;

		for (String key : keys) {
			JsonElement value = body.get(key);

			if (value != null && !value.isJsonNull()) {
				present++;
			}
		}

		// 全項目が未指定またはnullの場合は位置情報なし
		if (present == 0) {
			return;
		}

		if (present != keys.length) {
			throw new IllegalArgumentException();
		}

		double latitude = getNumber(body, "latitude");
		double longitude = getNumber(body, "longitude");
		double accuracy = getNumber(body, "locationAccuracy");

		String acquiredAtText = getString(body, "locationAcquiredAt");

		if (latitude < -90 || latitude > 90
				|| longitude < -180 || longitude > 180
				|| accuracy < 0
				|| acquiredAtText == null) {

			throw new IllegalArgumentException();
		}

		OffsetDateTime acquiredAt = OffsetDateTime.parse(acquiredAtText);

		report.setLatitude(latitude);
		report.setLongitude(longitude);
		report.setLocationAccuracy(accuracy);
		report.setLocationAcquiredAt(acquiredAt);
	}

	/**
	 * 通知を有効にしている関連保護者へFCMで通知する。
	 * 通知には氏名・回答内容・コメント・位置情報を含めない。
	 */
	private void notifyFamilies(long studentUserId) {

		try {
			AndroidPushSubscriptionDao dao = new AndroidPushSubscriptionDao();

			List<FamilyNotificationTarget> targets = dao.findFamilyTargets(studentUserId);

			if (targets.isEmpty()) {
				return;
			}

			FcmSender sender = new FcmSender();

			for (FamilyNotificationTarget target : targets) {
				try {
					// ログアウト・設定変更・登録の付け替えを再確認する
					if (!dao.isFamilyTargetEnabled(
							studentUserId, target)) {
						continue;
					}

					FcmSender.SendResult result = sender.send(
							target.getFcmToken(),
							"AnpiLink",
							"お子さんの安否回答が届きました。"
									+ "アプリで確認してください。");

					if (result.isUnregistered()) {
						dao.deleteUnregisteredFamilyTarget(target);

					} else if (result.isAccepted()) {
						// HTTP 200はFCMの受付成功を意味する
						LOGGER.log(
								Level.INFO,
								"保護者FCM受付成功: 登録ID={0}",
								target.getSubscriptionId());

					} else {
						LOGGER.log(
								Level.WARNING,
								"保護者FCM受付失敗: 登録ID={0}, HTTP={1}",
								new Object[] {
										target.getSubscriptionId(),
										result.getStatusCode()
								});
					}

				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();

					LOGGER.log(
							Level.WARNING,
							"保護者FCM送信を中断しました。");
					return;

				} catch (Exception e) {
					LOGGER.log(
							Level.WARNING,
							"保護者FCM送信失敗: 登録ID={0}, 例外={1}",
							new Object[] {
									target.getSubscriptionId(),
									e.getClass().getSimpleName()
							});
				}
			}

		} catch (Exception e) {
			LOGGER.log(
					Level.WARNING,
					"保護者FCM通知処理失敗: 例外={0}",
					e.getClass().getSimpleName());
		}
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