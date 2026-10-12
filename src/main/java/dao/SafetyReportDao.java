package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;

import javax.naming.NamingException;

import bean.SafetyReport;

/**
 * 安否回答情報を登録・取得するDAO。
 */
public class SafetyReportDao extends DAO {

    /**
     * 安否回答を新規登録する。
     * 成功した場合、回答IDと回答日時をBeanにセットする。
     */
    public boolean save(SafetyReport report)
            throws NamingException, SQLException {

        String sql = """
                INSERT INTO public.safety_report
                    (user_id, status, danger_type)
                VALUES (?, ?, ?)
                RETURNING id, reported_at
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            // 登録する値をセット
            statement.setLong(1, report.getUserId());
            statement.setString(2, report.getStatus());

            if (report.getDangerType() == null) {
                statement.setNull(3, Types.VARCHAR);
            } else {
                statement.setString(3, report.getDangerType());
            }

            // PostgreSQLのRETURNINGで登録結果を取得
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
     * 回答IDと利用者IDで安否回答を1件取得する。
     *
     * @return 該当する回答が存在しない場合はnull
     */
    public SafetyReport get(long id, long userId)
            throws NamingException, SQLException {

        String sql = """
                SELECT id,
                       user_id,
                       status,
                       danger_type,
                       reported_at
                FROM public.safety_report
                WHERE id = ?
                  AND user_id = ?
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            // 検索条件をセット
            statement.setLong(1, id);
            statement.setLong(2, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

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

                return report;
            }
        }
    }
}