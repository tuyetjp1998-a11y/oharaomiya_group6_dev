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
     * 安否回答と位置情報を登録する。
     */
    public boolean save(SafetyReport report)
            throws NamingException, SQLException {

        String sql = """
                INSERT INTO public.safety_report
                    (user_id, status, danger_type,
                     latitude, longitude, location_accuracy,
                     location_acquired_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id, reported_at
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, report.getUserId());
            statement.setString(2, report.getStatus());

            if (report.getDangerType() == null) {
                statement.setNull(3, Types.VARCHAR);
            } else {
                statement.setString(3, report.getDangerType());
            }

            setNullableDouble(
                    statement, 4, report.getLatitude());
            setNullableDouble(
                    statement, 5, report.getLongitude());
            setNullableDouble(
                    statement, 6, report.getLocationAccuracy());

            if (report.getLocationAcquiredAt() == null) {
                statement.setNull(
                        7, Types.TIMESTAMP_WITH_TIMEZONE);
            } else {
                statement.setObject(
                        7, report.getLocationAcquiredAt());
            }

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
     * 自分が登録した安否回答を取得する。
     */
    public SafetyReport get(long id, long userId)
            throws NamingException, SQLException {

        String sql = """
                SELECT id,
                       user_id,
                       status,
                       danger_type,
                       reported_at,
                       latitude,
                       longitude,
                       location_accuracy,
                       location_acquired_at
                FROM public.safety_report
                WHERE id = ?
                  AND user_id = ?
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

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

                report.setLatitude(
                        getNullableDouble(resultSet, "latitude"));
                report.setLongitude(
                        getNullableDouble(resultSet, "longitude"));
                report.setLocationAccuracy(
                        getNullableDouble(
                                resultSet, "location_accuracy"));

                report.setLocationAcquiredAt(
                        resultSet.getObject(
                                "location_acquired_at",
                                OffsetDateTime.class));

                return report;
            }
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
        return resultSet.wasNull() ? null : value;
    }
    
    /**
     * 利用者の最新の安否回答を取得する。
     *
     * @return 回答が存在しない場合はnull
     */
    public SafetyReport getLatestByUserId(long userId)
            throws NamingException, SQLException {

        String sql = """
                SELECT id,
                       user_id,
                       status,
                       danger_type,
                       reported_at,
                       latitude,
                       longitude,
                       location_accuracy,
                       location_acquired_at
                FROM public.safety_report
                WHERE user_id = ?
                ORDER BY reported_at DESC, id DESC
                LIMIT 1
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, userId);

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

                report.setLatitude(
                        getNullableDouble(resultSet, "latitude"));
                report.setLongitude(
                        getNullableDouble(resultSet, "longitude"));
                report.setLocationAccuracy(
                        getNullableDouble(
                                resultSet, "location_accuracy"));

                report.setLocationAcquiredAt(
                        resultSet.getObject(
                                "location_acquired_at",
                                OffsetDateTime.class));

                return report;
            }
        }
    }
}