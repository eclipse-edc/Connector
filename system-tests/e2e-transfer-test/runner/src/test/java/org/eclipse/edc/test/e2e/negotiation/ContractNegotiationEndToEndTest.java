/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.negotiation;

import org.eclipse.edc.connector.controlplane.test.system.utils.ManagementApiClientV4;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.annotations.Runtime;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.Runtimes;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.spi.query.Criterion.criterion;


@SuppressWarnings("JUnitMalformedDeclaration")
class ContractNegotiationEndToEndTest {

    abstract static class Tests {

        public static final String CONSUMER_ID = "urn:connector:consumer";
        public static final String PROVIDER_ID = "urn:connector:provider";
        public static final String CONSUMER_NAME = "consumer";
        public static final String PROVIDER_NAME = "provider";
        protected static String noConstraintPolicyId;

        @BeforeAll
        static void createNoConstraintPolicy(@Runtime(PROVIDER_NAME) ManagementApiClientV4 provider) {
            noConstraintPolicyId = provider.createPolicyDefinition(noConstraintPolicy());
        }

        private static @NotNull Map<String, Object> httpSourceDataAddress() {
            return new HashMap<>(Map.of(
                    "name", "transfer-test",
                    "baseUrl", "http://any/source",
                    "type", "HttpData"
            ));
        }

        protected void createResourcesOnProvider(ManagementApiClientV4 provider, String assetId, Map<String, Object> dataAddressProperties) {
            provider.createAsset(assetId, Map.of("description", "description"), dataAddressProperties);
            provider.createContractDefinition(assetId, UUID.randomUUID().toString(), noConstraintPolicyId, noConstraintPolicyId);
        }

        @Test
        void contractNegotiation(@Runtime(PROVIDER_NAME) ManagementApiClientV4 provider, @Runtime(CONSUMER_NAME) ManagementApiClientV4 consumer) {
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(provider, assetId, httpSourceDataAddress());

            var agreementId = consumer.negotiateContract(provider.asCounterParty(), assetId);

            var consumerAgreement = consumer.getContractAgreement(agreementId);

            var filter = criterion("agreementId", "=", consumerAgreement.getString("agreementId"));
            var providerAgreement = provider.queryContractAgreements(filter);

            assertThat(providerAgreement).hasSize(1).first().satisfies(jsonValue -> {
                var providerAgr = jsonValue.asJsonObject();

                assertThat(providerAgr.getString("consumerId")).isEqualTo(consumerAgreement.getString("consumerId"));
                assertThat(providerAgr.getString("providerId")).isEqualTo(consumerAgreement.getString("providerId"));
                assertThat(providerAgr.getString("assetId")).isEqualTo(consumerAgreement.getString("assetId"));
                assertThat(providerAgr.getJsonNumber("contractSignDate")).isEqualTo(consumerAgreement.getJsonNumber("contractSignDate"));
                assertThat(providerAgr.getJsonObject("policy"))
                        .usingRecursiveComparison().ignoringFields("@id")
                        .isEqualTo(consumerAgreement.getJsonObject("policy"));


            });

            assertThat(consumerAgreement).isNotNull();

        }

    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        static final RuntimeExtension CONSUMER_RUNTIME = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_NAME)
                .modules(Runtimes.ControlPlane.MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(() -> Runtimes.ControlPlane.config(CONSUMER_ID))
                .paramProvider(ManagementApiClientV4.class, ManagementApiClientV4::forContext)
                .build();

        @RegisterExtension
        static final RuntimeExtension PROVIDER_RUNTIME = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_NAME)
                .modules(Runtimes.ControlPlane.MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(() -> Runtimes.ControlPlane.config(PROVIDER_ID))
                .paramProvider(ManagementApiClientV4.class, ManagementApiClientV4::forContext)
                .build();

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback CREATE_DATABASES = context -> {
            POSTGRESQL_EXTENSION.createDatabase(CONSUMER_NAME);
            POSTGRESQL_EXTENSION.createDatabase(PROVIDER_NAME);
        };

        @RegisterExtension
        static final RuntimeExtension CONSUMER_RUNTIME = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_NAME)
                .modules(Runtimes.ControlPlane.MODULES)
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(() -> Runtimes.ControlPlane.config(CONSUMER_ID))
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(CONSUMER_NAME))
                .paramProvider(ManagementApiClientV4.class, ManagementApiClientV4::forContext)
                .build();

        @RegisterExtension
        static final RuntimeExtension PROVIDER_RUNTIME = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_NAME)
                .modules(Runtimes.ControlPlane.MODULES)
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(() -> Runtimes.ControlPlane.config(PROVIDER_ID))
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(PROVIDER_NAME))
                .paramProvider(ManagementApiClientV4.class, ManagementApiClientV4::forContext)
                .build();

    }

}
