package dao;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * データベース接続を取得するDAOの基底クラス。
 */
public class DAO {

    /**
     * Tomcatに登録されたDataSourceから接続を取得する。
     */
    public Connection getConnection()
            throws NamingException, SQLException {

        InitialContext context = new InitialContext();

        try {
            DataSource dataSource = (DataSource) context.lookup(
                    "java:comp/env/jdbc/safety");

            return dataSource.getConnection();

        } finally {
            context.close();
        }
    }
}