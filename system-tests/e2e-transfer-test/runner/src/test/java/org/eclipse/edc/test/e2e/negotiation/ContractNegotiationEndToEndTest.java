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
import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.policy.cel.service.CelPolicyExpressionService;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATED;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.atomicConstraint;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.policy;
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

        protected void createResourcesOnProvider(ManagementApiClientV4 provider, String assetId, Map<String, Object> dataAddressProperties, String accessPolicyId, String contractPolicyId) {
            provider.createAsset(assetId, Map.of("description", "description"), dataAddressProperties);
            provider.createContractDefinition(assetId, UUID.randomUUID().toString(), accessPolicyId, contractPolicyId);
        }

        protected void createResourcesOnProvider(ManagementApiClientV4 provider, String assetId, Map<String, Object> dataAddressProperties) {
            createResourcesOnProvider(provider, assetId, dataAddressProperties, noConstraintPolicyId, noConstraintPolicyId);
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

        @Test
        void contractNegotiation_withCelExpression(@Runtime(PROVIDER_NAME) ManagementApiClientV4 provider,
                                                   @Runtime(CONSUMER_NAME) ManagementApiClientV4 consumer,
                                                   @Runtime(PROVIDER_NAME) CelPolicyExpressionService expressionService) {
            var assetId = UUID.randomUUID().toString();

            expressionService.create(CelExpression.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .actions(Set.of("custom"))
                    .scopes(Set.of("catalog", "contract.negotiation"))
                    .leftOperand("test")
                    .expression("ctx.agent.id == '%s'".formatted(consumer.asCounterParty().participantId()))
                    .description("test expression")
                    .build());

            var permission = createObjectBuilder()
                    .add("action", "custom")
                    .add("constraint", atomicConstraint("test", "eq", "true"))
                    .build();


            var policyId = provider.createPolicyDefinition(policy(List.of(permission)));


            createResourcesOnProvider(provider, assetId, httpSourceDataAddress(), policyId, policyId);

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

        @Test
        void contractNegotiation_withCelExpression_failure(@Runtime(PROVIDER_NAME) ManagementApiClientV4 provider,
                                                           @Runtime(CONSUMER_NAME) ManagementApiClientV4 consumer,
                                                           @Runtime(PROVIDER_NAME) CelPolicyExpressionService expressionService) {
            var assetId = UUID.randomUUID().toString();

            expressionService.create(CelExpression.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .actions(Set.of("custom"))
                    .scopes(Set.of("catalog", "contract.negotiation"))
                    .leftOperand("test-failure")
                    .expression("ctx.agent.id != '%s'".formatted(consumer.asCounterParty().participantId()))
                    .description("test expression")
                    .build());

            var permission = createObjectBuilder()
                    .add("action", "custom")
                    .add("constraint", atomicConstraint("test-failure", "eq", "true"))
                    .build();


            var policyId = provider.createPolicyDefinition(policy(List.of(permission)));


            createResourcesOnProvider(provider, assetId, httpSourceDataAddress(), noConstraintPolicyId, policyId);

            var contractNegotiationId = consumer.initContractNegotiation(provider.asCounterParty(), assetId);


            await().untilAsserted(() -> {
                var state = consumer.getContractNegotiationState(contractNegotiationId);
                assertThat(state).isEqualTo(TERMINATED.name());
            });
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
                .modules(":core:common:cel-core")
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
                .modules(":core:common:cel-core")
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(() -> Runtimes.ControlPlane.config(PROVIDER_ID))
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(PROVIDER_NAME))
                .paramProvider(ManagementApiClientV4.class, ManagementApiClientV4::forContext)
                .build();

    }

}
