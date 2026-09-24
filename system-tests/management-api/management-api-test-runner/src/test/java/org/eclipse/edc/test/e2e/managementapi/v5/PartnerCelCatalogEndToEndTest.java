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

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.partner.spi.Partner;
import org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup;
import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerGroupStore;
import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerStore;
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
import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.policy.cel.store.CelExpressionStore;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_SCHEMA;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Restricts catalog visibility to partners through CEL expressions over {@code ctx.partners}: one scenario matches the
 * counterparty by identity and group membership, the other by a business identifier stored in the partner properties.
 */
public class PartnerCelCatalogEndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        private static final String PROVIDER = "provider-context";
        private static final String CONSUMER_A = "consumer-a";
        private static final String CONSUMER_B = "consumer-b";

        @BeforeEach
        void setup(ParticipantContextService participantContextService, ParticipantContextConfigStore configStore,
                   PartnerGroupStore groupStore, PartnerStore partnerStore) {
            createParticipant(participantContextService, configStore, PROVIDER);
            createParticipant(participantContextService, configStore, CONSUMER_A);
            createParticipant(participantContextService, configStore, CONSUMER_B);

            groupStore.create(PartnerGroup.Builder.newInstance().id("gold").participantContextId(PROVIDER).name("Gold").build());
            partnerStore.create(Partner.Builder.newInstance().id("consumer-a").participantContextId(PROVIDER)
                    .identity("did:web:" + CONSUMER_A).groupIds(Set.of("gold")).property("businessId", "BID-A").build());
            // consumer B is known but neither in the group nor carrying the business identifier
            partnerStore.create(Partner.Builder.newInstance().id("consumer-b").participantContextId(PROVIDER)
                    .identity("did:web:" + CONSUMER_B).property("businessId", "BID-B").build());
        }

        @AfterEach
        void teardown(ParticipantContextService participantContextService, PartnerStore partnerStore, PartnerGroupStore groupStore,
                      CelExpressionStore celExpressionStore, ContractDefinitionStore contractDefinitionStore,
                      PolicyDefinitionStore policyDefinitionStore, AssetIndex assetIndex) {
            partnerStore.query(QuerySpec.max()).forEach(p -> partnerStore.deleteById(p.getParticipantContextId(), p.getId()));
            groupStore.query(QuerySpec.max()).forEach(g -> groupStore.deleteById(g.getParticipantContextId(), g.getId()));
            celExpressionStore.query(QuerySpec.max()).forEach(e -> celExpressionStore.delete(e.getId()));
            contractDefinitionStore.findAll(QuerySpec.max()).toList().forEach(cd -> contractDefinitionStore.deleteById(cd.getParticipantContextId(), cd.getId()));
            policyDefinitionStore.findAll(QuerySpec.max()).toList().forEach(pd -> policyDefinitionStore.delete(pd.getParticipantContextId(), pd.getId()));
            assetIndex.queryAssets(QuerySpec.max()).toList().forEach(a -> assetIndex.deleteById(a.getParticipantContextId(), a.getId()));
            participantContextService.search(QuerySpec.max()).orElseThrow(f -> new AssertionError(f.getFailureDetail()))
                    .forEach(pc -> participantContextService.deleteParticipantContext(pc.getId()).orElseThrow(f -> new AssertionError(f.getFailureDetail())));
        }

        @Test
        void shouldRestrictCatalogToPartnerGroup(ManagementEndToEndV5TestContext context, OauthServer authServer,
                                                 CelExpressionStore celExpressionStore, AssetIndex assetIndex,
                                                 PolicyDefinitionStore policyDefinitionStore, ContractDefinitionStore contractDefinitionStore) {
            celExpressionStore.create(CelExpression.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .leftOperand("partnerGroup")
                    .expression("ctx.partners.byIdentity(ctx.agent.id).inGroup(this.rightOperand)")
                    .description("counterparty must belong to the partner group named by the right operand")
                    .build());
            var assetId = "asset-" + UUID.randomUUID();
            assetIndex.create(asset(assetId));
            createContractOffer(policyDefinitionStore, contractDefinitionStore, assetId, "partnerGroup", "gold");

            var catalogA = fetchCatalog(context, authServer, CONSUMER_A);
            var catalogB = fetchCatalog(context, authServer, CONSUMER_B);

            assertThat(datasetIds(catalogA)).containsExactly(assetId);
            assertThat(datasetIds(catalogB)).isEmpty();
        }

        @Test
        void shouldRestrictCatalogByPartnerProperty(ManagementEndToEndV5TestContext context, OauthServer authServer,
                                                    CelExpressionStore celExpressionStore, AssetIndex assetIndex,
                                                    PolicyDefinitionStore policyDefinitionStore, ContractDefinitionStore contractDefinitionStore) {
            celExpressionStore.create(CelExpression.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .leftOperand("partnerBusinessId")
                    .expression("ctx.partners.query({'businessId': this.rightOperand}).exists(p, p.identity == ctx.agent.id)")
                    .description("counterparty must be the partner registered with the given business identifier")
                    .build());
            var assetId = "asset-" + UUID.randomUUID();
            assetIndex.create(asset(assetId));
            createContractOffer(policyDefinitionStore, contractDefinitionStore, assetId, "partnerBusinessId", "BID-A");

            var catalogA = fetchCatalog(context, authServer, CONSUMER_A);
            var catalogB = fetchCatalog(context, authServer, CONSUMER_B);

            assertThat(datasetIds(catalogA)).containsExactly(assetId);
            assertThat(datasetIds(catalogB)).isEmpty();
        }

        private List<String> datasetIds(JsonObject catalog) {
            var datasets = catalog.get("dataset");
            if (datasets == null || datasets.getValueType() == JsonValue.ValueType.NULL) {
                return List.of();
            }
            if (datasets instanceof JsonArray array) {
                return array.stream().map(JsonValue::asJsonObject).map(d -> d.getString(ID)).toList();
            }
            return List.of(datasets.asJsonObject().getString(ID));
        }

        private JsonObject fetchCatalog(ManagementEndToEndV5TestContext context, OauthServer authServer, String consumerContextId) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "CatalogRequest")
                    .add("counterPartyAddress", context.providerProtocolUrl(PROVIDER, context.profile()))
                    .add("counterPartyId", PROVIDER)
                    .add("profile", context.profile())
                    .build()
                    .toString();

            return context.baseRequest(authServer.createToken(consumerContextId))
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5/participants/%s/catalog/request".formatted(consumerContextId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .extract().body().as(JsonObject.class);
        }

        private void createContractOffer(PolicyDefinitionStore policyStore, ContractDefinitionStore contractDefStore,
                                         String assetId, String leftOperand, String rightOperand) {
            var policyId = UUID.randomUUID().toString();
            var permission = Permission.Builder.newInstance()
                    .action(Action.Builder.newInstance().type(ODRL_SCHEMA + "use").build())
                    .constraint(AtomicConstraint.Builder.newInstance()
                            .leftExpression(new LiteralExpression(leftOperand))
                            .operator(Operator.EQ)
                            .rightExpression(new LiteralExpression(rightOperand))
                            .build())
                    .build();
            var policy = Policy.Builder.newInstance().permission(permission).build();

            policyStore.create(PolicyDefinition.Builder.newInstance().id(policyId).participantContextId(PROVIDER).policy(policy).build());
            contractDefStore.save(ContractDefinition.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .contractPolicyId(policyId)
                    .accessPolicyId(policyId)
                    .assetsSelectorCriterion(Criterion.criterion(EDC_NAMESPACE + "id", "=", assetId))
                    .participantContextId(PROVIDER)
                    .build());
        }

        private Asset asset(String id) {
            return Asset.Builder.newInstance()
                    .participantContextId(PROVIDER)
                    .id(id)
                    .dataAddress(DataAddress.Builder.newInstance().type("test-type").build())
                    .build();
        }

        private void createParticipant(ParticipantContextService participantContextService,
                                       ParticipantContextConfigStore configStore, String participantContextId) {
            var pc = ParticipantContext.Builder.newInstance()
                    .id(participantContextId)
                    .state(ParticipantContextState.ACTIVATED)
                    .identity(participantContextId)
                    .build();
            var config = ParticipantContextConfiguration.Builder.newInstance()
                    .participantContextId(participantContextId)
                    .entries(Map.of("edc.mock.region", "eu", "edc.participant.id", "did:web:" + participantContextId))
                    .build();
            configStore.save(config);
            participantContextService.createParticipantContext(pc).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
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
                .configurationProvider(Runtimes.ControlPlane::config)
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementEndToEndV5TestContext.class, ManagementEndToEndV5TestContext::forContext)
                .build();
    }
}
