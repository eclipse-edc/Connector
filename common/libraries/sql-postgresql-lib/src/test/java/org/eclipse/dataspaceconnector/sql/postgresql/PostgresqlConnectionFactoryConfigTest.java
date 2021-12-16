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

import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;

class PostgresqlConnectionFactoryConfigTest {

    @Test
    void test() {
        URI jdbcUri = URI.create("jdbc://localhost");
        String username = "username";
        String password = UUID.randomUUID().toString();
        String applicationName = UUID.randomUUID().toString();
        Boolean autoCommit = false;
        Long connectTimeout = 543L;
        Path loggerFile = Path.of("/dev/null");
        PostgresqlConnectionFactoryConfig.LoggerLevel loggerLevel = PostgresqlConnectionFactoryConfig.LoggerLevel.TRACE;
        Long loginTimeout = 252L;
        Boolean logUnclosedConnections = false;
        Boolean readOnly = false;
        Long socketTimeout = 1234L;
        Boolean ssl = false;
        Path sslCert = Path.of("/dev/null");
        String sslHostNameVerifier = "org.postgresql.ssl.PgjdbcHostnameVerifier";
        Path sslKey = Path.of("/dev/null");
        PostgresqlConnectionFactoryConfig.SslMode sslMode = PostgresqlConnectionFactoryConfig.SslMode.REQUIRE;
        Path sslRootCert = Path.of("/dev/null");

        PostgresqlConnectionFactoryConfig postgresqlConnectionFactoryConfig = PostgresqlConnectionFactoryConfig.Builder.newInstance()
                .uri(jdbcUri)
                .userName(username)
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

        Assertions.assertEquals(jdbcUri, postgresqlConnectionFactoryConfig.getUri());
        Assertions.assertEquals(username, postgresqlConnectionFactoryConfig.getUserName());
        Assertions.assertEquals(password, postgresqlConnectionFactoryConfig.getPassword());
        Assertions.assertEquals(applicationName, postgresqlConnectionFactoryConfig.getApplicationName());
        Assertions.assertEquals(autoCommit, postgresqlConnectionFactoryConfig.getAutoCommit());
        Assertions.assertEquals(connectTimeout, postgresqlConnectionFactoryConfig.getConnectTimeout());
        Assertions.assertEquals(loggerFile, postgresqlConnectionFactoryConfig.getLoggerFile());
        Assertions.assertEquals(loggerLevel, postgresqlConnectionFactoryConfig.getLoggerLevel());
        Assertions.assertEquals(loginTimeout, postgresqlConnectionFactoryConfig.getLoginTimeout());
        Assertions.assertEquals(logUnclosedConnections, postgresqlConnectionFactoryConfig.getLogUnclosedConnection());
        Assertions.assertEquals(readOnly, postgresqlConnectionFactoryConfig.getReadOnly());
        Assertions.assertEquals(socketTimeout, postgresqlConnectionFactoryConfig.getSocketTimeout());
        Assertions.assertEquals(ssl, postgresqlConnectionFactoryConfig.getSsl());
        Assertions.assertEquals(sslCert, postgresqlConnectionFactoryConfig.getSslCert());
        Assertions.assertEquals(sslHostNameVerifier, postgresqlConnectionFactoryConfig.getSslHostNameVerifier());
        Assertions.assertEquals(sslKey, postgresqlConnectionFactoryConfig.getSslKey());
        Assertions.assertEquals(sslMode, postgresqlConnectionFactoryConfig.getSslMode());
        Assertions.assertEquals(sslRootCert, postgresqlConnectionFactoryConfig.getSslRootCert());
    }
}