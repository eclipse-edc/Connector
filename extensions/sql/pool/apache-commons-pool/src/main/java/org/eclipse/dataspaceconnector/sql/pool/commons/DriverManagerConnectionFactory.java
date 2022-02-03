package org.eclipse.dataspaceconnector.sql.pool.commons;

import org.eclipse.dataspaceconnector.sql.ConnectionFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

public class DriverManagerConnectionFactory implements ConnectionFactory {
    private final String jdbcUrl;
    private final Properties properties;

    public DriverManagerConnectionFactory(String jdbcUrl, Properties properties) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public Connection create() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, properties);
    }
}
