/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.test.e2e;

import org.eclipse.dataspaceconnector.common.util.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.dataspaceconnector.common.util.postgres.PostgresqlLocalInstance;
import org.eclipse.dataspaceconnector.junit.extensions.EdcRuntimeExtension;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.dataspaceconnector.common.util.postgres.PostgresqlLocalInstance.PASSWORD;
import static org.eclipse.dataspaceconnector.common.util.postgres.PostgresqlLocalInstance.USER;

@PostgresqlDbIntegrationTest
class EndToEndTransferPostgresqlTest extends AbstractEndToEndTransfer {

    @RegisterExtension
    static EdcRuntimeExtension consumerControlPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:control-plane-postgresql",
            "consumer-control-plane",
            CONSUMER.controlPlanePostgresConfiguration()
    );

    @RegisterExtension
    static EdcRuntimeExtension consumerDataPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:data-plane",
            "consumer-data-plane",
            CONSUMER.dataPlaneConfiguration()
    );

    @RegisterExtension
    static EdcRuntimeExtension consumerBackendService = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:backend-service",
            "consumer-backend-service",
            new HashMap<>() {
                {
                    put("web.http.port", String.valueOf(CONSUMER.backendService().getPort()));
                }
            }
    );

    @RegisterExtension
    static EdcRuntimeExtension providerDataPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:data-plane",
            "provider-data-plane",
            PROVIDER.dataPlaneConfiguration()
    );

    @RegisterExtension
    static EdcRuntimeExtension providerControlPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:control-plane-postgresql",
            "provider-control-plane",
            PROVIDER.controlPlanePostgresConfiguration()
    );

    @RegisterExtension
    static EdcRuntimeExtension providerBackendService = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:backend-service",
            "provider-backend-service",
            new HashMap<>() {
                {
                    put("web.http.port", String.valueOf(PROVIDER.backendService().getPort()));
                }
            }
    );

    @BeforeAll
    static void beforeAll() throws SQLException, IOException, ClassNotFoundException {
        createDatabase(CONSUMER);
        createDatabase(PROVIDER);
    }

    private static void createDatabase(Participant consumer) throws ClassNotFoundException, SQLException, IOException {
        Class.forName("org.postgresql.Driver");

        PostgresqlLocalInstance.createDatabase(consumer.getName());

        var scripts = Stream.of(
                        "asset-index-sql",
                        "contract-definition-store-sql",
                        "contract-negotiation-store-sql",
                        "policy-store-sql",
                        "transfer-process-store-sql")
                .map(module -> "../../../extensions/sql/" + module + "/docs/schema.sql")
                .map(Paths::get)
                .collect(Collectors.toList());

        try (Connection connection = DriverManager.getConnection(consumer.jdbcUrl(), USER, PASSWORD)) {
            for (var script : scripts) {
                var sql = Files.readString(script);

                try (var statement = connection.createStatement()) {
                    statement.execute(sql);
                } catch (Exception exception) {
                    throw new EdcPersistenceException(exception.getMessage(), exception);
                }
            }
        }
    }
}
