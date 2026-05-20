/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.test.e2e.managementapi.v5;

import jakarta.json.JsonObject;
import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CatalogApiV5MultiProfileEndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {


        private static final List<String> PROFILES = List.of("dsp2025_1", "dsp2025_2");
        private static final AtomicConstraintRuleFunction<Permission, CatalogPolicyContext> POLICY_FUNCTION = mock();

        @BeforeAll
        static void setup(PolicyEngine policyEngine, RuleBindingRegistry ruleBindingRegistry) {

            ruleBindingRegistry.bind("use", "catalog");
            ruleBindingRegistry.bind("profile", "catalog");

            policyEngine.registerFunction(CatalogPolicyContext.class, Permission.class, "profile", POLICY_FUNCTION);
        }

        @AfterEach
        void teardown(ParticipantContextService participantContextService) {
            var list = participantContextService.search(QuerySpec.max())
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            for (var p : list) {
                participantContextService.deleteParticipantContext(p.getParticipantContextId()).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
            }
        }

        @Test
        void requestCatalogMultipleProfile(ManagementEndToEndV5TestContext context, ParticipantContextService participantContextService,
                                           ParticipantContextConfigStore configStore,
                                           AssetIndex assetIndex, PolicyDefinitionStore policyDefinitionStore,
                                           ContractDefinitionStore contractDefinitionStore, OauthServer authServer) {


            when(POLICY_FUNCTION.evaluate(any(), eq("dsp2025_1"), any(), any()))
                    .thenReturn(true)
                    .thenReturn(false);

            when(POLICY_FUNCTION.evaluate(any(), eq("dsp2025_2"), any(), any()))
                    .thenReturn(false)
                    .thenReturn(true);

            var providerContextId = "provider-context-" + UUID.randomUUID();
            var consumerContextId = "consumer-context-" + UUID.randomUUID();

            var participantTokenJwt = authServer.createToken(consumerContextId);

            createParticipant(participantContextService, configStore, consumerContextId, PROFILES);
            createParticipant(participantContextService, configStore, providerContextId, PROFILES);


            var firstAsset = "asset1";
            var secondAsset = "asset2";

            assetIndex.create(createAsset(providerContextId, firstAsset).build());
            assetIndex.create(createAsset(providerContextId, secondAsset).build());

            createContractOffer(providerContextId, policyDefinitionStore, contractDefinitionStore, firstAsset, "dsp2025_1");
            createContractOffer(providerContextId, policyDefinitionStore, contractDefinitionStore, secondAsset, "dsp2025_2");

            var firstCatalog = fetchCatalog(context, providerContextId, participantTokenJwt, consumerContextId, "dsp2025_1");

            assertCatalogContainsAsset(firstCatalog, firstAsset);
            var secondCatalog = fetchCatalog(context, providerContextId, participantTokenJwt, consumerContextId, "dsp2025_2");
            assertCatalogContainsAsset(secondCatalog, secondAsset);

        }

        private void assertCatalogContainsAsset(JsonObject catalog, String assetId) {
            var datasets = catalog.getJsonArray("dataset");
            var containsAsset = datasets.stream()
                    .filter(JsonObject.class::isInstance)
                    .map(JsonObject.class::cast)
                    .allMatch(dataset -> assetId.equals(dataset.getString(ID)));

            if (!containsAsset) {
                throw new AssertionError("Catalog does not contain  asset with id: %s, or contains asset with wrong id".formatted(assetId));
            }
        }

        private void createContractOffer(String participantContextId, PolicyDefinitionStore policyStore,
                                         ContractDefinitionStore contractDefStore, String assetId, String profile) {

            var policyId = UUID.randomUUID().toString();
            var permission = Permission.Builder.newInstance().action(Action.Builder.newInstance().type("use").build())

                    .constraint(AtomicConstraint.Builder.newInstance()
                            .leftExpression(new LiteralExpression("profile"))
                            .operator(Operator.EQ)
                            .rightExpression(new LiteralExpression(profile))
                            .build())
                    .build();
            var policy = Policy.Builder.newInstance()
                    .permission(permission)
                    .build();

            var contractDefinition = ContractDefinition.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .contractPolicyId(policyId)
                    .accessPolicyId(policyId)
                    .assetsSelectorCriterion(Criterion.criterion(EDC_NAMESPACE + "id", "=", assetId))
                    .participantContextId(participantContextId)
                    .build();


            policyStore.create(PolicyDefinition.Builder.newInstance().id(policyId)
                    .participantContextId(participantContextId)
                    .policy(policy).build());
            contractDefStore.save(contractDefinition);

        }

        private Asset.Builder createAsset(String participantContextId, String id) {
            return Asset.Builder.newInstance()
                    .participantContextId(participantContextId)
                    .id(id);
        }


        private void createParticipant(ParticipantContextService participantContextService,
                                       ParticipantContextConfigStore configStore, String participantContextId, List<String> profiles) {
            var pc = ParticipantContext.Builder.newInstance()
                    .participantContextId(participantContextId)
                    .state(ParticipantContextState.ACTIVATED)
                    .identity(participantContextId)
                    .build();

            var config = ParticipantContextConfiguration.Builder.newInstance()
                    .participantContextId(participantContextId)
                    .entries(Map.of("edc.mock.region", "eu",
                            "edc.participant.id", "did:web:" + participantContextId,
                            "edc.dataspace.profiles", String.join(",", profiles)
                    ))
                    .build();

            configStore.save(config);

            participantContextService.createParticipantContext(pc)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));
        }

        private JsonObject fetchCatalog(ManagementEndToEndV5TestContext context, String providerContextId, String participantTokenJwt, String consumerContextId, String profile) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "CatalogRequest")
                    .add("counterPartyAddress", context.providerProtocolUrl(providerContextId, profile))
                    .add("counterPartyId", providerContextId)
                    .add("profile", profile)
                    .build()
                    .toString();

            return context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/catalog/request".formatted(consumerContextId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .extract().body().as(JsonObject.class);
        }

    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @Order(0)
        @RegisterExtension
        static final OauthServerEndToEndExtension AUTH_SERVER_EXTENSION = OauthServerEndToEndExtension.Builder.newInstance().build();

        @Order(1)
        @RegisterExtension
        static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.VIRTUAL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(InMemory::config)
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementEndToEndV5TestContext.class,
                        ManagementEndToEndV5TestContext::forContext)
                .build();

        static Config config() {
            return ConfigFactory.fromMap(Map.of(
                            "edc.dataspace.profiles.dsp2025_1.name", "dsp2025_1",
                            "edc.dataspace.profiles.dsp2025_1.protocolVersion", "2025-1",
                            "edc.dataspace.profiles.dsp2025_1.protocolBinding", "HTTPS",
                            "edc.dataspace.profiles.dsp2025_1.protocolNamespace", "https://w3id.org/dspace/2025/1/",
                            "edc.dataspace.profiles.dsp2025_1.jsonLdContextsUrl", "https://w3id.org/dspace/2025/1/context.jsonld",
                            "edc.dataspace.profiles.dsp2025_2.name", "dsp2025_2",
                            "edc.dataspace.profiles.dsp2025_2.protocolVersion", "2025-1",
                            "edc.dataspace.profiles.dsp2025_2.protocolBinding", "HTTPS",
                            "edc.dataspace.profiles.dsp2025_2.protocolNamespace", "https://w3id.org/dspace/2025/1/",
                            "edc.dataspace.profiles.dsp2025_2.jsonLdContextsUrl", "https://w3id.org/dspace/2025/1/context.jsonld"
                    )
            );
        }
    }

}