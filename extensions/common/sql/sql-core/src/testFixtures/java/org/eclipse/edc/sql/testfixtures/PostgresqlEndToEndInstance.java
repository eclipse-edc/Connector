/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.sql.testfixtures;

import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public interface PostgresqlEndToEndInstance {

    String USER = "postgres";
    String PASSWORD = "password";
    String JDBC_URL_PREFIX = "jdbc:postgresql://localhost:5432/";

    static void createDatabase(String participantName) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new EdcPersistenceException(e);
        }

        var postgres = new PostgresqlLocalInstance(USER, PASSWORD, JDBC_URL_PREFIX, participantName);
        postgres.createDatabase();

        var extensionsFolder = TestUtils.findBuildRoot().toPath().resolve("extensions");
        var scripts = Stream.of(
                        "control-plane/store/sql/asset-index-sql/src/main/resources/asset-index-schema.sql",
                        "control-plane/store/sql/contract-definition-store-sql/src/main/resources/contract-definition-schema.sql",
                        "control-plane/store/sql/contract-negotiation-store-sql/src/main/resources/contract-negotiation-schema.sql",
                        "control-plane/store/sql/policy-definition-store-sql/src/main/resources/policy-definition-schema.sql",
                        "control-plane/store/sql/transfer-process-store-sql/src/main/resources/transfer-process-schema.sql",
                        "data-plane/store/sql/data-plane-store-sql/src/main/resources/dataplane-schema.sql",
                        "policy-monitor/store/sql/policy-monitor-store-sql/src/main/resources/policy-monitor-schema.sql",
                        "common/store/sql/edr-index-sql/src/main/resources/edr-index-schema.sql"
                )
                .map(extensionsFolder::resolve)
                .toList();

        try (var connection = postgres.getConnection(participantName)) {
            for (var script : scripts) {
                var sql = Files.readString(script);

                try (var statement = connection.createStatement()) {
                    statement.execute(sql);
                } catch (Exception exception) {
                    throw new EdcPersistenceException(exception.getMessage(), exception);
                }
            }
        } catch (SQLException | IOException e) {
            throw new EdcPersistenceException(e);
        }

    }

    static Map<String, String> defaultDatasourceConfiguration(String name) {
        return new HashMap<>() {
            {
                put("edc.datasource.default.url", JDBC_URL_PREFIX + name);
                put("edc.datasource.default.user", USER);
                put("edc.datasource.default.password", PASSWORD);
            }
        };
    }

}
