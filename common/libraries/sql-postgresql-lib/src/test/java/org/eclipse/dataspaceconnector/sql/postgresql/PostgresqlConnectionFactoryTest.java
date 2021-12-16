/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Test
 *
 */

package org.eclipse.dataspaceconnector.sql.postgresql;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.URI;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

class PostgresqlConnectionFactoryTest {
    private static final String JDBC_URI_STRING = "jdbc://uri-is-mandatory";
    private static final URI JDBC_URI = URI.create(JDBC_URI_STRING);
    private static final String USERNAME = "username is mandatory";

    @Test
    void testConstructorNullConfigThrowsNullPointerException() {
        Assertions.assertThrows(NullPointerException.class, () -> new PostgresqlConnectionFactory(null));
    }

    @Test
    void testConstructor() {
        PostgresqlConnectionFactoryConfig postgresqlConnectionFactoryConfig = PostgresqlConnectionFactoryConfig.Builder.newInstance()
                .uri(JDBC_URI)
                .userName(USERNAME)
                .build();
        PostgresqlConnectionFactory postgresqlConnectionFactory = new PostgresqlConnectionFactory(postgresqlConnectionFactoryConfig);

        Assertions.assertNotNull(postgresqlConnectionFactory);
    }

    @Test
    void testCreate() throws SQLException {
        try (MockedStatic<DriverManager> driverManager = Mockito.mockStatic(DriverManager.class)) {
            PostgresqlConnectionFactoryConfig postgresqlConnectionFactoryConfig = PostgresqlConnectionFactoryConfig.Builder.newInstance()
                    .uri(JDBC_URI)
                    .userName(USERNAME)
                    .build();
            PostgresqlConnectionFactory postgresqlConnectionFactory = new PostgresqlConnectionFactory(postgresqlConnectionFactoryConfig);

            Connection connection = Mockito.mock(Connection.class);
            Mockito.when(DriverManager.getConnection(Mockito.eq(JDBC_URI_STRING), Mockito.any(Properties.class))).thenReturn(connection);

            Connection result = postgresqlConnectionFactory.create();

            Assertions.assertNotNull(result);
            Assertions.assertEquals(connection, result);

            driverManager.verify(
                    () -> DriverManager.getConnection(Mockito.eq(JDBC_URI_STRING), Mockito.any(Properties.class)),
                    Mockito.times(1)
            );
        }
    }

    @Test
    void testCreateConnectionProperties() throws SQLException {
        try (MockedStatic<DriverManager> driverManager = Mockito.mockStatic(DriverManager.class)) {
            String password = UUID.randomUUID().toString();
            String applicationName = UUID.randomUUID().toString();
            Boolean autoCommit = true;
            Long connectTimeout = 1234L;
            Path loggerFile = Path.of("/dev/null");
            PostgresqlConnectionFactoryConfig.LoggerLevel loggerLevel = PostgresqlConnectionFactoryConfig.LoggerLevel.DEBUG;
            Long loginTimeout = 345L;
            Boolean logUnclosedConnections = true;
            Boolean readOnly = true;
            Long socketTimeout = 1234L;
            Boolean ssl = true;
            Path sslCert = Path.of("/dev/null");
            String sslHostNameVerifier = "org.postgresql.ssl.PgjdbcHostnameVerifier";
            Path sslKey = Path.of("/dev/null");
            PostgresqlConnectionFactoryConfig.SslMode sslMode = PostgresqlConnectionFactoryConfig.SslMode.PREFER;
            Path sslRootCert = Path.of("/dev/null");

            PostgresqlConnectionFactoryConfig postgresqlConnectionFactoryConfig = PostgresqlConnectionFactoryConfig.Builder.newInstance()
                    .uri(JDBC_URI)
                    .userName(USERNAME)
                    .password(password)
                    .applicationName(applicationName)
                    .autoCommit(autoCommit)
                    .connectTimeout(connectTimeout)
                    .loggerFile(loggerFile)
                    .loggerLevel(loggerLevel)
                    .loginTimeout(loginTimeout)
                    .logUnclosedConnections(logUnclosedConnections)
                    .readOnly(readOnly)
                    .socketTimeout(socketTimeout)
                    .ssl(ssl)
                    .sslCert(sslCert)
                    .sslHostNameVerifier(sslHostNameVerifier)
                    .sslKey(sslKey)
                    .sslMode(sslMode)
                    .sslRootCert(sslRootCert)
                    .build();
            PostgresqlConnectionFactory postgresqlConnectionFactory = new PostgresqlConnectionFactory(postgresqlConnectionFactoryConfig);

            Connection connection = Mockito.mock(Connection.class);
            Properties properties = new Properties() {
                {
                    put("user", USERNAME);
                    put("password", password);
                    put("ssl", ssl.toString());
                    put("sslmode", sslMode.toString());
                    put("sslcert", sslCert.toString());
                    put("sslkey", sslKey.toString());
                    put("sslrootcert", sslRootCert.toString());
                    put("sslhostnameverifier", sslHostNameVerifier);
                    put("loggerLevel", loggerLevel.toString());
                    put("loggerFile", loggerFile.toString());
                    put("logUnclosedConnections", logUnclosedConnections.toString());
                    put("connectTimeout", connectTimeout.toString());
                    put("socketTimeout", socketTimeout.toString());
                    put("loginTimeout", loginTimeout.toString());
                    put("readOnly", readOnly.toString());
                    put("ApplicationName", applicationName);
                }
            };

            driverManager.when(() -> DriverManager.getConnection(Mockito.eq(JDBC_URI_STRING), Mockito.eq(properties))).thenReturn(connection);

            Connection result = postgresqlConnectionFactory.create();

            Assertions.assertNotNull(result);
            Assertions.assertEquals(connection, result);

            driverManager.verify(
                    () -> DriverManager.getConnection(Mockito.eq(JDBC_URI_STRING), Mockito.eq(properties)),
                    Mockito.times(1)
            );
        }
    }
}