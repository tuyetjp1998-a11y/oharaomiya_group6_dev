package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.naming.NamingException;

import bean.WorriedType;

/** 「困っている」の全校共通項目を管理する。 */
public class WorriedTypeDao extends DAO {

    /** 有効な項目を登録順で取得する。 */
    public List<WorriedType> findActive()
            throws NamingException, SQLException {

        String sql = """
                SELECT id, code, name
                FROM public.worried_type
                WHERE active = true
                ORDER BY id
                """;
        List<WorriedType> items = new ArrayList<>();

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                WorriedType item = new WorriedType();
                item.setId(resultSet.getLong("id"));
                item.setCode(resultSet.getString("code"));
                item.setName(resultSet.getString("name"));
                items.add(item);
            }
        }
        return items;
    }

    /** 項目を追加する。同名の有効項目は重複登録しない。 */
    public boolean add(String name, long userId)
            throws NamingException, SQLException {

        String sql = """
                INSERT INTO public.worried_type
                    (code, name, created_by)
                VALUES (?, ?, ?)
                ON CONFLICT DO NOTHING
                """;
        String code = "CUSTOM_"
                + UUID.randomUUID().toString()
                        .replace("-", "")
                        .toUpperCase();

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, code);
            statement.setString(2, name);
            statement.setLong(3, userId);
            return statement.executeUpdate() == 1;
        }
    }

    /** 履歴を残したまま選択肢から外す。 */
    public String deactivate(long id, long userId)
            throws NamingException, SQLException {

        String sql = """
                UPDATE public.worried_type
                SET active = false,
                    deleted_by = ?,
                    deleted_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND active = true
                RETURNING name
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, userId);
            statement.setLong(2, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? resultSet.getString("name")
                        : null;
            }
        }
    }
}
