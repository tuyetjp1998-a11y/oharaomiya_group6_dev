package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.NamingException;

/**
 * ホームから送信した危険回答を30秒以内に取り消す。
 */
public class SafetyReportCancellationDao extends DAO {

    public enum CancelResult {
        CANCELLED,
        ALREADY_CANCELLED,
        NOT_CANCELLABLE,
        NOT_FOUND
    }

    /**
     * 本人の危険回答だけを取り消す。
     * 取り消し済みの同じ回答への再要求は成功扱いにする。
     */
    public CancelResult cancel(
            long reportId,
            long userId)
            throws NamingException, SQLException {

        try (Connection connection = getConnection()) {

            boolean originalAutoCommit =
                    connection.getAutoCommit();

            connection.setAutoCommit(false);

            try {
                // 同じ回答への取り消し処理を直列化する。
                boolean exists = lockReport(
                        connection,
                        reportId,
                        userId);

                if (!exists) {
                    boolean alreadyCancelled =
                            wasCancelled(
                                    connection,
                                    reportId,
                                    userId);

                    connection.commit();

                    return alreadyCancelled
                            ? CancelResult.ALREADY_CANCELLED
                            : CancelResult.NOT_FOUND;
                }

                // ロック取得後にDBの現在時刻で期限を判定する。
                boolean archived = archiveReport(
                        connection,
                        reportId,
                        userId);

                if (!archived) {
                    connection.rollback();
                    return CancelResult.NOT_CANCELLABLE;
                }

                String deleteSql = """
                        DELETE FROM public.safety_report
                        WHERE id = ?
                          AND user_id = ?
                        """;

                try (PreparedStatement statement =
                        connection.prepareStatement(deleteSql)) {

                    statement.setLong(1, reportId);
                    statement.setLong(2, userId);

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException(
                                "危険回答の取り消し更新に失敗しました。");
                    }
                }

                connection.commit();

                return CancelResult.CANCELLED;

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
     * 本人の回答をロックする。
     */
    private boolean lockReport(
            Connection connection,
            long reportId,
            long userId) throws SQLException {

        String sql = """
                SELECT id
                FROM public.safety_report
                WHERE id = ?
                  AND user_id = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement =
                connection.prepareStatement(sql)) {

            statement.setLong(1, reportId);
            statement.setLong(2, userId);

            try (ResultSet resultSet =
                    statement.executeQuery()) {

                return resultSet.next();
            }
        }
    }

    /**
     * 本人が取り消した回答か確認する。
     */
    private boolean wasCancelled(
            Connection connection,
            long reportId,
            long userId) throws SQLException {

        String sql = """
                SELECT report_id
                FROM public.safety_report_cancellation
                WHERE report_id = ?
                  AND user_id = ?
                """;

        try (PreparedStatement statement =
                connection.prepareStatement(sql)) {

            statement.setLong(1, reportId);
            statement.setLong(2, userId);

            try (ResultSet resultSet =
                    statement.executeQuery()) {

                return resultSet.next();
            }
        }
    }

    /**
     * 期限内の危険回答を履歴へ保存する。
     * 種類付きの旧回答や無事回答は対象にしない。
     */
    private boolean archiveReport(
            Connection connection,
            long reportId,
            long userId) throws SQLException {

        String sql = """
                INSERT INTO public.safety_report_cancellation
                    (report_id,
                     user_id,
                     report_snapshot,
                     cancelled_at)
                SELECT r.id,
                       r.user_id,
                       to_jsonb(r),
                       clock_timestamp()
                FROM public.safety_report r
                WHERE r.id = ?
                  AND r.user_id = ?
                  AND r.status = 'DANGER'
                  AND r.danger_type IS NULL
                  AND r.reported_at <= clock_timestamp()
                  AND r.reported_at >
                      clock_timestamp() - INTERVAL '30 seconds'
                RETURNING report_id
                """;

        try (PreparedStatement statement =
                connection.prepareStatement(sql)) {

            statement.setLong(1, reportId);
            statement.setLong(2, userId);

            try (ResultSet resultSet =
                    statement.executeQuery()) {

                return resultSet.next();
            }
        }
    }
}