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

package org.eclipse.edc.test.e2e;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlDbIntegrationTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static java.time.Duration.ofDays;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.test.system.utils.PolicyFixtures.inForceDatePolicy;
import static org.eclipse.edc.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.hamcrest.CoreMatchers.equalTo;

public class TransferPullEndToEndTest {

    @Nested
    @EndToEndTest
    class InMemory extends Tests implements InMemoryRuntimes {

    }

    @Nested
    @PostgresqlDbIntegrationTest
    class Postgres extends Tests implements PostgresRuntimes {

    }

    abstract static class Tests extends TransferEndToEndTestBase {

        @BeforeEach
        void setUp() {
            PROVIDER.registerDataPlane();
        }

        @Test
        void pullFromHttp() {
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, noConstraintPolicy(), httpDataAddressProperties());
            var dynamicReceiverProps = CONSUMER.dynamicReceiverPrivateProperties();

            var transferProcessId = CONSUMER.requestAsset(PROVIDER, assetId, dynamicReceiverProps, syncDataAddress());
            await().atMost(timeout).untilAsserted(() -> {
                var state = CONSUMER.getTransferProcessState(transferProcessId);
                assertThat(state).isEqualTo(STARTED.name());
            });

            // retrieve the data reference
            var edr = CONSUMER.getDataReference(transferProcessId);

            // pull the data without query parameter
            await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(edr, Map.of(), equalTo("some information")));

            // pull the data with additional query parameter
            var msg = UUID.randomUUID().toString();
            await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(edr, Map.of("message", msg), equalTo(msg)));

            // We expect two EDR because in the test runtime we have two receiver extensions static and dynamic one
            assertThat(CONSUMER.getAllDataReferences(transferProcessId)).hasSize(2);
        }

        @Test
        void pullFromHttp_withTransferType() {
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, noConstraintPolicy(), httpDataAddressProperties());
            var dynamicReceiverProps = CONSUMER.dynamicReceiverPrivateProperties();

            var transferProcessId = CONSUMER.requestAsset(PROVIDER, assetId, dynamicReceiverProps, syncDataAddress(), "HttpData-PULL");
            await().atMost(timeout).untilAsserted(() -> {
                var state = CONSUMER.getTransferProcessState(transferProcessId);
                assertThat(state).isEqualTo(STARTED.name());
            });

            // retrieve the data reference
            var edr = CONSUMER.getDataReference(transferProcessId);

            // pull the data without query parameter
            await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(edr, Map.of(), equalTo("some information")));

            // pull the data with additional query parameter
            var msg = UUID.randomUUID().toString();
            await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(edr, Map.of("message", msg), equalTo(msg)));

            // We expect two EDR because in the test runtime we have two receiver extensions static and dynamic one
            assertThat(CONSUMER.getAllDataReferences(transferProcessId)).hasSize(2);
        }

        @Test
        void shouldTerminateTransfer_whenContractExpires_fixedInForcePeriod() {
            var assetId = UUID.randomUUID().toString();
            var now = Instant.now();

            // contract was valid from t-10d to t-5d, so "now" it is expired
            var contractPolicy = inForceDatePolicy("gteq", now.minus(ofDays(10)), "lteq", now.minus(ofDays(5)));
            createResourcesOnProvider(assetId, contractPolicy, httpDataAddressProperties());

            var transferProcessId = CONSUMER.requestAsset(PROVIDER, assetId, noPrivateProperty(), syncDataAddress());
            await().atMost(timeout).untilAsserted(() -> {
                var state = CONSUMER.getTransferProcessState(transferProcessId);
                assertThat(state).isEqualTo(TERMINATED.name());
            });
        }

        @Test
        void shouldTerminateTransfer_whenContractExpires_durationInForcePeriod() {
            var assetId = UUID.randomUUID().toString();
            var now = Instant.now();
            // contract was valid from t-10d to t-5d, so "now" it is expired
            var contractPolicy = inForceDatePolicy("gteq", now.minus(ofDays(10)), "lteq", "contractAgreement+1s");
            createResourcesOnProvider(assetId, contractPolicy, httpDataAddressProperties());

            var transferProcessId = CONSUMER.requestAsset(PROVIDER, assetId, noPrivateProperty(), syncDataAddress());
            await().atMost(timeout).untilAsserted(() -> {
                var state = CONSUMER.getTransferProcessState(transferProcessId);
                assertThat(state).isEqualTo(TERMINATED.name());
            });
        }

        @Test
        void pullFromHttp_httpProvision() {
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, noConstraintPolicy(), Map.of(
                    "name", "transfer-test",
                    "baseUrl", PROVIDER.backendService() + "/api/provider/data",
                    "type", "HttpProvision",
                    "proxyQueryParams", "true"
            ));

            var transferProcessId = CONSUMER.requestAsset(PROVIDER, assetId, noPrivateProperty(), syncDataAddress());
            await().atMost(timeout).untilAsserted(() -> {
                var state = CONSUMER.getTransferProcessState(transferProcessId);
                assertThat(state).isEqualTo(STARTED.name());

                var edr = CONSUMER.getDataReference(transferProcessId);
                CONSUMER.pullData(edr, Map.of(), equalTo("some information"));
            });
        }

        private JsonObject syncDataAddress() {
            return createObjectBuilder()
                    .add(TYPE, EDC_NAMESPACE + "DataAddress")
                    .add(EDC_NAMESPACE + "type", "HttpProxy")
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

        private JsonObject noPrivateProperty() {
            return Json.createObjectBuilder().build();
        }
    }
}
