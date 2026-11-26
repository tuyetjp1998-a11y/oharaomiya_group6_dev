package dao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.UUID;

import javax.naming.NamingException;

import bean.SafetyReport;

/**
 * 安否回答・位置情報・コメントを登録、取得する。
 */
public class SafetyReportDao extends DAO {

	private static final String SELECT_REPORT = """
			SELECT id,
			       user_id,
			       status,
			       danger_type,
			       reported_at,
			       latitude,
			       longitude,
			       location_accuracy,
			       location_acquired_at,
			       comment
			FROM public.safety_report
			""";

	/**
	 * 安否回答を登録する。
	 */
	public boolean save(SafetyReport report)
			throws NamingException, SQLException {

		String sql = """
				INSERT INTO public.safety_report
				    (user_id,
				     status,
				     danger_type,
				     latitude,
				     longitude,
				     location_accuracy,
				     location_acquired_at,
				     comment)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				RETURNING id, reported_at
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, report.getUserId());
			statement.setString(2, report.getStatus());

			setNullableString(
					statement,
					3,
					report.getDangerType());

			setNullableDouble(
					statement,
					4,
					report.getLatitude());

			setNullableDouble(
					statement,
					5,
					report.getLongitude());

			setNullableDouble(
					statement,
					6,
					report.getLocationAccuracy());

			if (report.getLocationAcquiredAt() == null) {
				statement.setNull(
						7,
						Types.TIMESTAMP_WITH_TIMEZONE);
			} else {
				statement.setObject(
						7,
						report.getLocationAcquiredAt());
			}

			setNullableString(
					statement,
					8,
					report.getComment());

			try (ResultSet resultSet = statement.executeQuery()) {

				if (!resultSet.next()) {
					return false;
				}

				report.setId(resultSet.getLong("id"));

				report.setReportedAt(
						resultSet.getObject(
								"reported_at",
								OffsetDateTime.class));

				return true;
			}
		}
	}

	/**
	 * 本人が登録した回答を取得する。
	 */
	public SafetyReport get(
			long id,
			long userId)
			throws NamingException, SQLException {

		String sql = SELECT_REPORT + """
				WHERE id = ?
				  AND user_id = ?
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, id);
			statement.setLong(2, userId);

			try (ResultSet resultSet = statement.executeQuery()) {

				return resultSet.next()
						? mapReport(resultSet)
						: null;
			}
		}
	}

	/**
	 * 利用者の最新の有効な回答を取得する。
	 * 取り消した回答は別の履歴テーブルに保存されている。
	 */
	public SafetyReport getLatestByUserId(long userId)
			throws NamingException, SQLException {

		String sql = SELECT_REPORT + """
				WHERE user_id = ?
				ORDER BY reported_at DESC, id DESC
				LIMIT 1
				""";

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, userId);

			try (ResultSet resultSet = statement.executeQuery()) {

				return resultSet.next()
						? mapReport(resultSet)
						: null;
			}
		}
	}

	/**
	 * リクエストID付きで回答を登録する。
	 * 回答と重複防止記録を同じトランザクションで保存する。
	 */
	public SubmissionResult saveOnce(
			SafetyReport report,
			UUID requestId,
			String payloadHash)
			throws NamingException, SQLException {

        return saveOnce(report, requestId, payloadHash, null);
    }

    /** 配信に対する回答を登録する。配信IDなしの既存送信にも対応する。 */
    public SubmissionResult saveOnce(SafetyReport report, UUID requestId,
            String payloadHash, Long dispatchId)
            throws NamingException, SQLException {

		if (report == null
				|| report.getUserId() <= 0
				|| requestId == null
				|| payloadHash == null
				|| !payloadHash.matches("[0-9a-f]{64}")) {

			throw new IllegalArgumentException(
					"送信識別情報が正しくありません。");
		}

        if (dispatchId != null) {
            if (dispatchId <= 0) {
                throw new IllegalArgumentException("配信IDが正しくありません。");
            }
            // 配信IDも重複判定の対象に含める。旧APIのハッシュは変更しない。
            try {
                payloadHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest((payloadHash + ":dispatch:" + dispatchId)
                                .getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }

		// 同じ利用者の同時送信を順番に処理する。
		String lockUserSql = """
				SELECT id
				FROM public.users
				WHERE id = ?
				  AND active = TRUE
				  AND role = 'STUDENT'
				FOR UPDATE
				""";

		String findSubmissionSql = """
				SELECT payload_hash, report_id
				FROM public.safety_report_submission
				WHERE user_id = ?
				  AND request_id = ?
				""";

		String insertReportSql = """
				INSERT INTO public.safety_report
				    (user_id,
				     status,
				     danger_type,
				     latitude,
				     longitude,
				     location_accuracy,
				     location_acquired_at,
				     comment, dispatch_id)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
				RETURNING id, reported_at
				""";

		String insertSubmissionSql = """
				INSERT INTO public.safety_report_submission
				    (user_id, request_id, payload_hash, report_id)
				VALUES (?, ?, ?, ?)
				""";

		try (Connection connection = getConnection()) {
			boolean originalAutoCommit = connection.getAutoCommit();

			connection.setAutoCommit(false);

			try {
				try (PreparedStatement statement = connection.prepareStatement(lockUserSql)) {

					statement.setLong(1, report.getUserId());

					try (ResultSet resultSet = statement.executeQuery()) {

						if (!resultSet.next()) {
							connection.rollback();

							return new SubmissionResult(
									SubmissionStatus.FORBIDDEN,
									null);
						}
					}
				}

				String previousHash = null;
				Long previousReportId = null;

				try (PreparedStatement statement = connection.prepareStatement(
						findSubmissionSql)) {

					statement.setLong(1, report.getUserId());
					statement.setObject(2, requestId);

					try (ResultSet resultSet = statement.executeQuery()) {

						if (resultSet.next()) {
							previousHash = resultSet.getString(
									"payload_hash");

							previousReportId = resultSet.getLong(
									"report_id");
						}
					}
				}

				if (previousReportId != null) {
					// 同じIDで内容を変更した要求は受け付けない。
					if (!payloadHash.equals(previousHash)) {
						connection.rollback();

						return new SubmissionResult(
								SubmissionStatus.CONFLICT,
								null);
					}

					SafetyReport previousReport = null;

					String findReportSql = SELECT_REPORT + """
							WHERE id = ?
							  AND user_id = ?
							""";

					try (PreparedStatement statement = connection.prepareStatement(
							findReportSql)) {

						statement.setLong(1, previousReportId);
						statement.setLong(2, report.getUserId());

						try (ResultSet resultSet = statement.executeQuery()) {

							if (resultSet.next()) {
								previousReport = mapReport(resultSet);
							}
						}
					}

					connection.commit();

					// 取り消しなどで削除済みの場合も再登録しない。
					if (previousReport == null) {
						return new SubmissionResult(
								SubmissionStatus.GONE,
								null);
					}

					return new SubmissionResult(
							SubmissionStatus.REPLAYED,
							previousReport);
				}

                // 本人が配信対象であることを確認する。期限後の回答も受け付ける。
                if (dispatchId != null) {
                    String recipientSql = """
                            SELECT 1 FROM public.safety_dispatch_recipient r
                            JOIN public.safety_dispatch d ON d.id = r.dispatch_id
                            WHERE r.dispatch_id = ? AND r.user_id = ?
                              AND d.deadline_at IS NOT NULL
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(recipientSql)) {
                        statement.setLong(1, dispatchId);
                        statement.setLong(2, report.getUserId());
                        try (ResultSet rs = statement.executeQuery()) {
                            if (!rs.next()) {
                                connection.rollback();
                                return new SubmissionResult(SubmissionStatus.FORBIDDEN, null);
                            }
                        }
                    }
                }

				long savedId;
				OffsetDateTime savedAt;

				try (PreparedStatement statement = connection.prepareStatement(
						insertReportSql)) {

					statement.setLong(1, report.getUserId());
					statement.setString(2, report.getStatus());

					setNullableString(
							statement, 3,
							report.getDangerType());

					setNullableDouble(
							statement, 4,
							report.getLatitude());

					setNullableDouble(
							statement, 5,
							report.getLongitude());

					setNullableDouble(
							statement, 6,
							report.getLocationAccuracy());

					if (report.getLocationAcquiredAt() == null) {
						statement.setNull(
								7,
								Types.TIMESTAMP_WITH_TIMEZONE);
					} else {
						statement.setObject(
								7,
								report.getLocationAcquiredAt());
					}

					setNullableString(
							statement, 8,
							report.getComment());
                    if (dispatchId == null) {
                        statement.setNull(9, Types.BIGINT);
                    } else {
                        statement.setLong(9, dispatchId);
                    }


					try (ResultSet resultSet = statement.executeQuery()) {

						if (!resultSet.next()) {
							throw new SQLException(
									"回答の登録結果を取得できませんでした。");
						}

						savedId = resultSet.getLong("id");

						savedAt = resultSet.getObject(
								"reported_at",
								OffsetDateTime.class);
					}
				}

				try (PreparedStatement statement = connection.prepareStatement(
						insertSubmissionSql)) {

					statement.setLong(1, report.getUserId());
					statement.setObject(2, requestId);
					statement.setString(3, payloadHash);
					statement.setLong(4, savedId);

					if (statement.executeUpdate() != 1) {
						throw new SQLException(
								"送信識別情報を保存できませんでした。");
					}
				}

				connection.commit();

				// コミット成功後だけBeanに保存結果を反映する。
				report.setId(savedId);
				report.setReportedAt(savedAt);

				return new SubmissionResult(
						SubmissionStatus.CREATED,
						report);

			} catch (SQLException | RuntimeException e) {
				try {
					connection.rollback();
				} catch (SQLException rollbackError) {
					e.addSuppressed(rollbackError);
				}

				throw e;

			} finally {
				connection.setAutoCommit(originalAutoCommit);
			}
		}
	}

	/**
	 * リクエストID付き登録の処理結果。
	 */
	public enum SubmissionStatus {
		CREATED, REPLAYED, CONFLICT, GONE, FORBIDDEN
	}

	/**
	 * 新規登録時、再要求時は保存済みの回答を返す。
	 */
	public static final class SubmissionResult {

		private final SubmissionStatus status;
		private final SafetyReport report;

		public SubmissionResult(
				SubmissionStatus status,
				SafetyReport report) {

			this.status = status;
			this.report = report;
		}

		public SubmissionStatus getStatus() {
			return status;
		}

		public SafetyReport getReport() {
			return report;
		}
	}

	/**
	 * 検索結果をBeanに設定する。
	 */
	private SafetyReport mapReport(
			ResultSet resultSet) throws SQLException {

		SafetyReport report = new SafetyReport();

		report.setId(resultSet.getLong("id"));
		report.setUserId(resultSet.getLong("user_id"));
		report.setStatus(resultSet.getString("status"));
		report.setDangerType(
				resultSet.getString("danger_type"));

		report.setReportedAt(
				resultSet.getObject(
						"reported_at",
						OffsetDateTime.class));

		report.setLatitude(
				getNullableDouble(
						resultSet,
						"latitude"));

		report.setLongitude(
				getNullableDouble(
						resultSet,
						"longitude"));

		report.setLocationAccuracy(
				getNullableDouble(
						resultSet,
						"location_accuracy"));

		report.setLocationAcquiredAt(
				resultSet.getObject(
						"location_acquired_at",
						OffsetDateTime.class));

		report.setComment(
				resultSet.getString("comment"));

		return report;
	}

	private void setNullableString(
			PreparedStatement statement,
			int index,
			String value) throws SQLException {

		if (value == null) {
			statement.setNull(index, Types.VARCHAR);
		} else {
			statement.setString(index, value);
		}
	}

	private void setNullableDouble(
			PreparedStatement statement,
			int index,
			Double value) throws SQLException {

		if (value == null) {
			statement.setNull(index, Types.DOUBLE);
		} else {
			statement.setDouble(index, value);
		}
	}

	private Double getNullableDouble(
			ResultSet resultSet,
			String column) throws SQLException {

		double value = resultSet.getDouble(column);

		return resultSet.wasNull()
				? null
				: value;
	}
}