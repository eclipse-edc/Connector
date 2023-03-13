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

import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import org.eclipse.edc.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.edc.azure.testfixtures.CosmosTestClient;
import org.eclipse.edc.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@AzureCosmosDbIntegrationTest
class EndToEndTransferCosmosDbTest extends AbstractEndToEndTransfer {

    public static final String E2E_TEST_NAME = "e2e-transfer-test-" + UUID.randomUUID();
    @RegisterExtension
    static EdcRuntimeExtension consumerControlPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:control-plane-cosmosdb",
            "consumer-control-plane",
            CONSUMER.controlPlaneCosmosDbConfiguration(E2E_TEST_NAME)
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
            ":system-tests:e2e-transfer-test:control-plane-cosmosdb",
            "provider-control-plane",
            PROVIDER.controlPlaneCosmosDbConfiguration(E2E_TEST_NAME)
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
    private static CosmosDatabase database;

    @BeforeAll
    static void beforeAll() {
        var client = CosmosTestClient.createClient();
        var response = client.createDatabaseIfNotExists(E2E_TEST_NAME);
        database = client.getDatabase(response.getProperties().getId());

        Stream.of("provider", "consumer")
                .flatMap(str -> Stream.of(
                        str + "-assetindex",
                        str + "-contractdefinitionstore",
                        str + "-contractnegotiationstore",
                        str + "-nodedirectory",
                        str + "-policystore",
                        str + "-transfer-process-store"))

                .map(name -> database.createContainerIfNotExists(name, "/partitionKey"))
                .map(r -> database.getContainer(r.getProperties().getId()))
                .forEach(container -> {
                    var api = new CosmosDbApiImpl(container, false);
                    api.uploadStoredProcedure("nextForState");
                    api.uploadStoredProcedure("lease");
                });

    }

    @AfterAll
    static void cleanup() {
        if (database != null) {
            CosmosDatabaseResponse delete = database.delete();
            assertThat(delete.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(300);
        }
    }
}
