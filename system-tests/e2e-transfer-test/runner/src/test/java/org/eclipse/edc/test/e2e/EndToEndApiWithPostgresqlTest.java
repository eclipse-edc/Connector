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

package org.eclipse.edc.test.e2e;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.testfixtures.PostgresqlLocalInstance;
import org.eclipse.edc.test.e2e.participant.EndToEndTransferParticipant;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.test.e2e.PostgresConstants.JDBC_URL_PREFIX;
import static org.eclipse.edc.test.e2e.PostgresConstants.PASSWORD;
import static org.eclipse.edc.test.e2e.PostgresConstants.USER;
import static org.eclipse.edc.test.system.utils.PolicyFixtures.noConstraintPolicy;

@PostgresqlDbIntegrationTest
class EndToEndApiWithPostgresqlTest {

    protected static final EndToEndTransferParticipant CONSUMER = EndToEndTransferParticipant.Builder.newInstance()
            .name("consumer")
            .id("urn:connector:consumer")
            .build();
    protected static final EndToEndTransferParticipant PROVIDER = EndToEndTransferParticipant.Builder.newInstance()
            .name("provider")
            .id("urn:connector:provider")
            .build();

    @RegisterExtension
    static EdcRuntimeExtension consumerControlPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:control-plane-postgresql",
            "consumer-control-plane",
            CONSUMER.controlPlanePostgresConfiguration()
    );

    @RegisterExtension
    static EdcRuntimeExtension providerControlPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:control-plane-postgresql",
            "provider-control-plane",
            PROVIDER.controlPlanePostgresConfiguration()
    );


    @BeforeAll
    static void beforeAll() throws SQLException, IOException, ClassNotFoundException {
        createDatabase(CONSUMER);
        createDatabase(PROVIDER);
    }

    private static void createDatabase(EndToEndTransferParticipant consumer) throws ClassNotFoundException, SQLException, IOException {
        Class.forName("org.postgresql.Driver");

        var helper = new PostgresqlLocalInstance(USER, PASSWORD, JDBC_URL_PREFIX, consumer.getName());
        helper.createDatabase(consumer.getName());

        var scripts = Stream.of(
                        "asset-index-sql",
                        "contract-definition-store-sql",
                        "contract-negotiation-store-sql",
                        "policy-definition-store-sql",
                        "transfer-process-store-sql")
                .map(module -> "../../../extensions/control-plane/store/sql/" + module + "/docs/schema.sql")
                .map(Paths::get)
                .toList();


        try (var connection = DriverManager.getConnection(consumer.jdbcUrl(), USER, PASSWORD)) {
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

    @Test
    void testQueryTransferProcess_byState() {
        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(assetId, noConstraintPolicy(), httpDataAddressProperties());
        var destination = httpDataAddress(CONSUMER.backendService() + "/api/consumer/store");

        CONSUMER.requestAsset(PROVIDER, assetId, Json.createObjectBuilder().build(), destination);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var result = PROVIDER.queryTransfer(STARTED);
            assertThat(result).isNotEmpty();
            assertThat(result.getJsonObject(0).getString("edc:state")).isEqualTo("STARTED");
        });
    }

    private JsonObject httpDataAddress(String baseUrl) {
        return createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "properties", createObjectBuilder()
                        .add(EDC_NAMESPACE + "baseUrl", baseUrl)
                        .build())
                .build();
    }

    @NotNull
    private Map<String, Object> httpDataAddressProperties() {
        return Map.of(
                "name", "transfer-test",
                "baseUrl", PROVIDER.backendService() + "/api/provider/data",
                "type", "HttpData",
                "proxyQueryParams", "true"
        );
    }

    private void createResourcesOnProvider(String assetId, JsonObject contractPolicy, Map<String, Object> dataAddressProperties) {
        PROVIDER.createAsset(assetId, Map.of("description", "description"), dataAddressProperties);
        var accessPolicyId = PROVIDER.createPolicyDefinition(noConstraintPolicy());
        var contractPolicyId = PROVIDER.createPolicyDefinition(contractPolicy);
        PROVIDER.createContractDefinition(assetId, UUID.randomUUID().toString(), accessPolicyId, contractPolicyId);
    }
}
