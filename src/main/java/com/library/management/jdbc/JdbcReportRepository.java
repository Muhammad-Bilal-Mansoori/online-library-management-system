package com.library.management.jdbc;

import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
* Direct JDBC access used for Review 1 database-integration demonstration.
* The main application uses JPA for normal CRUD operations; this class
* demonstrates direct SQL execution through JDBC.
*/
@Repository
public class JdbcReportRepository {
    private final DataSource dataSource;
    public JdbcReportRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    public int getTotalBooks() {
        return queryInt("SELECT COALESCE(SUM(total_copies), 0) FROM books");
    }
    public int getAvailableBooks() {
        return queryInt("SELECT COALESCE(SUM(available_copies), 0) FROM books");
    }
    public int getActiveMembers() {
        return queryInt(
        "SELECT COUNT(*) FROM users WHERE role = ? AND active = TRUE",
        "MEMBER"
        );
    }
    private int queryInt(String sql, Object... parameters) {
        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
        catch (Exception e) {
            throw new IllegalStateException("JDBC query failed: " + sql, e);
        }
    }
}
