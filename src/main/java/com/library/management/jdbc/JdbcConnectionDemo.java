package com.library.management.jdbc;

import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
* Small JDBC connectivity demonstration for the college Review 1.
* It opens a JDBC connection and executes a simple parameterized SQL query.
*/
@Component
public class JdbcConnectionDemo {
    private final DataSource dataSource;
    public JdbcConnectionDemo(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    public String checkConnection() {
        String sql = "SELECT COUNT(*) FROM books";
        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return "JDBC connection successful. Books in database: " + resultSet.getInt(1);
            }
            return "JDBC connection successful.";
        }
        catch (Exception e) {
            return "JDBC connection failed: " + e.getMessage();
        }
    }
}
