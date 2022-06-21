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

import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.dataspaceconnector.azure.testfixtures.CosmosTestClient;
import org.eclipse.dataspaceconnector.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.dataspaceconnector.junit.extensions.EdcRuntimeExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.stream.Stream;

@AzureCosmosDbIntegrationTest
class EndToEndTransferCosmosDbTest extends AbstractEndToEndTransfer {

    @RegisterExtension
    static EdcRuntimeExtension consumerControlPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:control-plane-cosmosdb",
            "consumer-control-plane",
            CONSUMER.controlPlaneCosmosDbConfiguration()
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
            PROVIDER.controlPlaneCosmosDbConfiguration()
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
    static void beforeAll() {
        var client = CosmosTestClient.createClient();
        var response = client.createDatabaseIfNotExists("e2e-transfer-test");
        var database = client.getDatabase(response.getProperties().getId());

        Stream.of(
                        "assetindex",
                        "contractdefinitionstore",
                        "contractnegotiationstore",
                        "nodedirectory",
                        "policystore",
                        "transfer-process-store")
                .map(name -> database.createContainerIfNotExists(name, "/partitionKey"))
                .map(r -> database.getContainer(r.getProperties().getId()))
                .forEach(container -> {
                    var api = new CosmosDbApiImpl(container, false);
                    api.uploadStoredProcedure("nextForState");
                    api.uploadStoredProcedure("lease");
                });

    }
}
