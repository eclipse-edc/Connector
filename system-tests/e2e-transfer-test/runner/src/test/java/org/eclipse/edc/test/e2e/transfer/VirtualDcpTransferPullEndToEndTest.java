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

package org.eclipse.edc.test.e2e.transfer;

import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participants;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.ManagementApiClientV5;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.AtomicConstraintDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.CatalogDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.CatalogRequestDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.CelExpressionDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.ContractNegotiationDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.CriterionDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.DatasetDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.DatasetRequestDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.PermissionDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.PolicyDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.QuerySpectDto;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.iam.decentralizedclaims.spi.credentialservice.CredentialService;
import org.eclipse.edc.iam.decentralizedclaims.spi.credentialservice.CredentialServiceEndToEndExtension;
import org.eclipse.edc.iam.decentralizedclaims.spi.issuerservice.IssuerService;
import org.eclipse.edc.iam.decentralizedclaims.spi.issuerservice.IssuerServiceEndToEndExtension;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.CatalogScopeExtractor;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.CatalogScopeExtractorRegistry;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.annotations.Runtime;
import org.eclipse.edc.junit.extensions.ComponentRuntimeContext;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.nats.testfixtures.NatsEndToEndExtension;
import org.eclipse.edc.protocol.spi.RequestContext;
import org.eclipse.edc.signaling.auth.Oauth2Extension;
import org.eclipse.edc.signaling.client.DataPlaneSignalingTestClient;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.Runtimes;
import org.eclipse.edc.test.e2e.fixtures.VaultApi;
import org.eclipse.edc.test.e2e.fixtures.VaultEndToEndExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.ApprovalContractNegotiationPolicyContext.APPROVAL_SCOPE;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.CONSUMER_DP;
import static org.eclipse.edc.test.e2e.transfer.VirtualTransferEndToEndTestBase.CONSUMER_CONTEXT;
import static org.eclipse.edc.test.e2e.transfer.VirtualTransferEndToEndTestBase.PROVIDER_CONTEXT;


class VirtualDcpTransferPullEndToEndTest {


    private static final String CONSUMER_VAULT_KEY = "consumer-" + UUID.randomUUID();
    private static final String CATALOG_ACCESS_CREDENTIAL = "CatalogAccessCredential";
    private static final String CATALOG_ACCESS_SCOPE = "org.eclipse.dspace.dcp.vc.type:%s:read".formatted(CATALOG_ACCESS_CREDENTIAL);
    private static final CatalogScopeExtractor NO_SCOPES = context -> Set.of();
    /**
     * Delegate of the {@link CatalogScopeExtractor} registered in the runtime, that can be changed by each test.
     */
    private static final AtomicReference<CatalogScopeExtractor> CATALOG_SCOPE_EXTRACTOR = new AtomicReference<>(NO_SCOPES);

    private static Participants participants(ComponentRuntimeContext ctx, CredentialServiceEndToEndExtension ext) {
        var protocolEndpoint = ctx.getEndpoint("protocol");
        var signalingEndpoint = ctx.getEndpoint("signaling");
        var providerDid = ext.didFor(PROVIDER_CONTEXT);
        var providerCfg = ext.dcpConfig(PROVIDER_CONTEXT);
        var consumerDid = ext.didFor(CONSUMER_CONTEXT);
        var consumerCfg = ext.dcpConfig(CONSUMER_CONTEXT).merge(stsSignatureConfig(CONSUMER_VAULT_KEY, consumerDid + "#additional-key"));

        return new Participants(
                new Participants.Participant(PROVIDER_CONTEXT, providerDid, protocolEndpoint, signalingEndpoint, providerCfg.getEntries()),
                new Participants.Participant(CONSUMER_CONTEXT, consumerDid, protocolEndpoint, signalingEndpoint, consumerCfg.getEntries())
        );
    }

    private static Config stsSignatureConfig(String keyName, String kid) {
        return ConfigFactory.fromMap(Map.of(
                "edc.iam.sts.type", "signature",
                "edc.iam.sts.signature.keyname", keyName,
                "edc.iam.sts.signature.kid", kid
        ));
    }

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class DcpTransferPullEndToEndTestBase extends VirtualTransferEndToEndTestBase {

        /**
         * Set up the test environment by creating one issuer, two participants in their
         * respective Identity Hubs, and issuing a MembershipCredential credential for each participant.
         * The consumer also gets a CatalogAccessCredential, which is not part of the default scopes.
         */
        @BeforeAll
        static void setup(IssuerService issuer,
                          CredentialService credentialService,
                          Participants participants,
                          @Runtime(Runtimes.ControlPlane.NAME) Vault vault,
                          @Runtime(Runtimes.ControlPlane.NAME) CatalogScopeExtractorRegistry catalogScopeExtractorRegistry,
                          VaultApi vaultApi) {

            vaultApi.enableTransitEngine();

            var consumerContextId = participants.consumer().contextId();
            var providerContextId = participants.provider().contextId();
            vault.storeSecret(consumerContextId, "%s-alias".formatted(consumerContextId), "%s-sts-secret".formatted(consumerContextId));
            vault.storeSecret(providerContextId, "%s-alias".formatted(providerContextId), "%s-sts-secret".formatted(providerContextId));

            var key = vaultApi.addTransitEngineKey(CONSUMER_VAULT_KEY, participants.consumer().id() + "#additional-key");

            credentialService.addParticipant(participants.consumer().contextId(), key);
            credentialService.addParticipant(participants.provider().contextId());

            var consumerMembershipCredential = issuer.issueCredential(participants.consumer().id(), "MembershipCredential", Map.of("status", "active"));
            var providerMembershipCredential = issuer.issueCredential(participants.provider().id(), "MembershipCredential", Map.of("status", "active"));

            credentialService.storeCredential(participants.consumer().contextId(), consumerMembershipCredential);
            credentialService.storeCredential(participants.provider().contextId(), providerMembershipCredential);

            var consumerCatalogAccessCredential = issuer.issueCredential(participants.consumer().id(), CATALOG_ACCESS_CREDENTIAL, Map.of("status", "active"));
            credentialService.storeCredential(participants.consumer().contextId(), consumerCatalogAccessCredential);

            catalogScopeExtractorRegistry.register(context -> CATALOG_SCOPE_EXTRACTOR.get().extractScopes(context));
        }

        @AfterEach
        void resetCatalogScopeExtractor() {
            CATALOG_SCOPE_EXTRACTOR.set(NO_SCOPES);
        }

        @Test
        void catalog_shouldNotContainOffer_whenCatalogScopeIsNotRequested(ManagementApiClientV5 connectorClient,
                                                                           Participants participants) {
            var assetId = setupCatalogAccessAsset(connectorClient, participants);

            var catalog = requestCatalog(connectorClient, participants, List.of(CATALOG_ACCESS_SCOPE));

            assertThat(catalog.datasets()).extracting(DatasetDto::id).doesNotContain(assetId);
        }

        @Test
        void catalog_shouldContainOffer_whenProviderRequestsCatalogScope_andConsumerSendsAdditionalScopes(ManagementApiClientV5 connectorClient,
                                                                                                           Participants participants) {
            var assetId = setupCatalogAccessAsset(connectorClient, participants);
            CATALOG_SCOPE_EXTRACTOR.set(scopesFor(participants.provider().contextId(), RequestContext.Direction.Ingress));

            var catalog = requestCatalog(connectorClient, participants, List.of(CATALOG_ACCESS_SCOPE));

            assertThat(catalog.datasets()).extracting(DatasetDto::id).contains(assetId);
        }

        @Test
        void catalog_shouldContainOffer_whenCatalogScopeIsExtractedOnBothSides(ManagementApiClientV5 connectorClient,
                                                                                Participants participants) {
            var assetId = setupCatalogAccessAsset(connectorClient, participants);
            var providerScopes = scopesFor(participants.provider().contextId(), RequestContext.Direction.Ingress);
            var consumerScopes = scopesFor(participants.consumer().contextId(), RequestContext.Direction.Egress);
            CATALOG_SCOPE_EXTRACTOR.set(context -> union(providerScopes.extractScopes(context), consumerScopes.extractScopes(context)));

            var catalog = requestCatalog(connectorClient, participants, null);

            assertThat(catalog.datasets()).extracting(DatasetDto::id).contains(assetId);
        }

        @Test
        void dataset_shouldContainOffer_whenCatalogScopeIsExtractedOnBothSides(ManagementApiClientV5 connectorClient,
                                                                                Participants participants) {
            var assetId = setupCatalogAccessAsset(connectorClient, participants);
            var providerScopes = scopesFor(participants.provider().contextId(), RequestContext.Direction.Ingress);
            var consumerScopes = scopesFor(participants.consumer().contextId(), RequestContext.Direction.Egress);
            CATALOG_SCOPE_EXTRACTOR.set(context -> union(providerScopes.extractScopes(context), consumerScopes.extractScopes(context)));

            var datasetRequest = new DatasetRequestDto(assetId, participants.consumer().profile(),
                    participants.provider().getProtocolEndpoint(), participants.provider().id());
            var dataset = connectorClient.catalogs().getDataset(participants.consumer().contextId(), datasetRequest);

            assertThat(dataset.id()).isEqualTo(assetId);
            assertThat(dataset.offers()).isNotEmpty();
        }

        @Test
        void catalog_shouldFail_whenProviderRequestsCatalogScopeNotGrantedByConsumer(ManagementApiClientV5 connectorClient,
                                                                                    Participants participants) {
            setupCatalogAccessAsset(connectorClient, participants);
            CATALOG_SCOPE_EXTRACTOR.set(scopesFor(participants.provider().contextId(), RequestContext.Direction.Ingress));

            var request = new CatalogRequestDto(participants.consumer().profile(),
                    participants.provider().getProtocolEndpoint(), participants.provider().id());

            connectorClient.catalogs().requestCatalogResponse(participants.consumer().contextId(), request)
                    .statusCode(502);
        }

        private String setupCatalogAccessAsset(ManagementApiClientV5 connectorClient, Participants participants) {
            var leftOperand = "https://w3id.org/example/credentials/CatalogAccessCredential/" + UUID.randomUUID();
            var expression = "ctx.agent.claims.vc.hasCredential('%s')".formatted(CATALOG_ACCESS_CREDENTIAL);
            connectorClient.expressions().createExpression(new CelExpressionDto(leftOperand, expression, Set.of("catalog"), "catalog access expression"));

            var accessPolicy = new PolicyDto(List.of(new PermissionDto(new AtomicConstraintDto(leftOperand, "eq", "active"))));
            return setup(connectorClient, participants.provider(), accessPolicy);
        }

        private CatalogDto requestCatalog(ManagementApiClientV5 connectorClient, Participants participants, List<String> additionalScopes) {
            var request = new CatalogRequestDto(participants.consumer().profile(),
                    participants.provider().getProtocolEndpoint(), participants.provider().id(), additionalScopes);
            return connectorClient.catalogs().requestCatalog(participants.consumer().contextId(), request);
        }

        private CatalogScopeExtractor scopesFor(String participantContextId, RequestContext.Direction direction) {
            return context -> {
                var requestContext = context.requestContext();
                if (participantContextId.equals(requestContext.getParticipantContextId()) && requestContext.getDirection() == direction) {
                    return Set.of(CATALOG_ACCESS_SCOPE);
                }
                return Set.of();
            };
        }

        private Set<String> union(Set<String> first, Set<String> second) {
            var result = new HashSet<>(first);
            result.addAll(second);
            return result;
        }


        @Test
        void httpPull_dataTransfer_withMembershipExpression(ManagementApiClientV5 connectorClient,
                                                            Participants participants) {

            var leftOperand = "https://w3id.org/example/credentials/MembershipCredential";
            var expression = """
                    ctx.agent.claims.vc
                    .exists(c, c.type.exists(t, t == 'MembershipCredential'))
                    """;

            var scopes = Set.of("catalog", "contract.negotiation", "transfer.process");
            var expr = new CelExpressionDto(leftOperand, expression, scopes, "membership expression");
            connectorClient.expressions().createExpression(expr);

            var providerAddress = participants.provider().getProtocolEndpoint();

            var constraint = new AtomicConstraintDto(leftOperand, "eq", "active");
            var permission = new PermissionDto(constraint);
            var policy = new PolicyDto(List.of(permission));

            var assetId = setup(connectorClient, participants.provider(), policy);
            var transferProcessId = connectorClient.startTransfer(participants.consumer().contextId(), participants.consumer().profile(), participants.provider().contextId(), providerAddress, participants.provider().id(), assetId, "NonFinite-PULL");

            var consumerTransfer = connectorClient.transfers().getTransferProcess(participants.consumer().contextId(), transferProcessId);
            var providerTransfer = connectorClient.transfers().getTransferProcess(participants.provider().contextId(), consumerTransfer.getCorrelationId());

            assertThat(consumerTransfer.getState()).isEqualTo(providerTransfer.getState());

        }

        @Test
        void httpPull_dataTransfer_withMembershipHelperFunctions(ManagementApiClientV5 connectorClient,
                                                                 Participants participants) {

            var leftOperand = "https://w3id.org/example/credentials/MembershipCredentialHelpers";
            // short form equivalent of the filter/exists expression above, using the VC helper functions
            var expression = "ctx.agent.claims.vc.valid().withType('MembershipCredential').hasClaim('status', 'active')";

            var scopes = Set.of("catalog", "contract.negotiation", "transfer.process");
            var expr = new CelExpressionDto(leftOperand, expression, scopes, "membership helper expression");
            connectorClient.expressions().createExpression(expr);

            var providerAddress = participants.provider().getProtocolEndpoint();

            var constraint = new AtomicConstraintDto(leftOperand, "eq", "active");
            var permission = new PermissionDto(constraint);
            var policy = new PolicyDto(List.of(permission));

            var assetId = setup(connectorClient, participants.provider(), policy);
            var transferProcessId = connectorClient.startTransfer(participants.consumer().contextId(), participants.consumer().profile(), participants.provider().contextId(), providerAddress, participants.provider().id(), assetId, "NonFinite-PULL");

            var consumerTransfer = connectorClient.transfers().getTransferProcess(participants.consumer().contextId(), transferProcessId);
            var providerTransfer = connectorClient.transfers().getTransferProcess(participants.provider().contextId(), consumerTransfer.getCorrelationId());

            assertThat(consumerTransfer.getState()).isEqualTo(providerTransfer.getState());
        }

        @Test
        void negotiation_fails_withMissingCredentialHelperFunction(ManagementApiClientV5 connectorClient,
                                                                   Participants participants) {

            var leftOperand = "https://w3id.org/example/credentials/DataAccessCredentialHelpers";
            var expression = "ctx.agent.claims.vc.hasCredential('DataAccessCredential')";

            var expr = new CelExpressionDto(leftOperand, expression, Set.of("contract.negotiation"), "data credential helper expression");
            connectorClient.expressions().createExpression(expr);

            var providerAddress = participants.provider().getProtocolEndpoint();

            var constraint = new AtomicConstraintDto(leftOperand, "eq", "active");
            var permission = new PermissionDto(constraint);
            var policy = new PolicyDto(List.of(permission));

            var assetId = setup(connectorClient, participants.provider(), policy);
            var negotiationId = connectorClient.initContractNegotiation(participants.consumer().contextId(), participants.consumer().profile(), assetId, providerAddress, participants.provider().id());

            connectorClient.waitForContractNegotiationState(participants.consumer().contextId(), negotiationId, ContractNegotiationStates.TERMINATED.name());
            var error = connectorClient.getNegotiationError(participants.consumer().contextId(), negotiationId);

            assertThat(error).isNotNull().contains("Unauthorized");
        }

        @Test
        void negotiation_fails_withMissingCredential(ManagementApiClientV5 connectorClient,
                                                     Participants participants) {

            var leftOperand = "https://w3id.org/example/credentials/DataAccessCredential";
            var expression = """
                    ctx.agent.claims.vc
                    .exists(c, c.type.exists(t, t == 'DataAccessCredential'))
                    """;

            var expr = new CelExpressionDto(leftOperand, expression, Set.of("contract.negotiation"), "data credential expression");
            connectorClient.expressions().createExpression(expr);

            var providerAddress = participants.provider().getProtocolEndpoint();

            var constraint = new AtomicConstraintDto(leftOperand, "eq", "active");
            var permission = new PermissionDto(constraint);
            var policy = new PolicyDto(List.of(permission));

            var assetId = setup(connectorClient, participants.provider(), policy);
            var negotiationId = connectorClient.initContractNegotiation(participants.consumer().contextId(), participants.consumer().profile(), assetId, providerAddress, participants.provider().id());

            connectorClient.waitForContractNegotiationState(participants.consumer().contextId(), negotiationId, ContractNegotiationStates.TERMINATED.name());
            var error = connectorClient.getNegotiationError(participants.consumer().contextId(), negotiationId);

            assertThat(error).isNotNull().contains("Unauthorized");

        }

        @Test
        void policyMonitor_shouldNotTerminateTransfer_whenCredentialIsValid(ManagementApiClientV5 connectorClient,
                                                                            Participants participants) {

            // evaluated only by the policy monitor, against the claims stored on the agreement
            var leftOperand = "https://w3id.org/example/monitor/MembershipCredential";
            var expression = "ctx.agent.claims.vc.valid().withType('MembershipCredential').hasClaim('status', 'active')";

            var expr = new CelExpressionDto(leftOperand, expression, Set.of("policy.monitor"), "membership monitor expression");
            connectorClient.expressions().createExpression(expr);

            var providerAddress = participants.provider().getProtocolEndpoint();
            var policy = new PolicyDto(List.of(new PermissionDto(new AtomicConstraintDto(leftOperand, "eq", "active"))));

            var assetId = setup(connectorClient, participants.provider(), policy);
            var transferProcessId = connectorClient.startTransfer(participants.consumer().contextId(), participants.consumer().profile(), participants.provider().contextId(), providerAddress, participants.provider().id(), assetId, "NonFinite-PULL");

            // the policy monitor evaluates the agreement policy multiple times during this period
            await().during(Duration.ofSeconds(5)).atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                    assertThat(connectorClient.transfers().getTransferProcess(participants.consumer().contextId(), transferProcessId).getState())
                            .isEqualTo(TransferProcessStates.STARTED.name()));
        }

        @Test
        void policyMonitor_shouldTerminateTransfer_whenCredentialIsMissing(ManagementApiClientV5 connectorClient,
                                                                           Participants participants) {

            var leftOperand = "https://w3id.org/example/monitor/DataAccessCredential";
            var expression = "ctx.agent.claims.vc.hasCredential('DataAccessCredential')";

            var expr = new CelExpressionDto(leftOperand, expression, Set.of("policy.monitor"), "data access monitor expression");
            connectorClient.expressions().createExpression(expr);

            var providerAddress = participants.provider().getProtocolEndpoint();
            var policy = new PolicyDto(List.of(new PermissionDto(new AtomicConstraintDto(leftOperand, "eq", "active"))));

            var assetId = setup(connectorClient, participants.provider(), policy);
            var transferProcessId = connectorClient.startTransfer(participants.consumer().contextId(), participants.consumer().profile(), participants.provider().contextId(), providerAddress, participants.provider().id(), assetId, "NonFinite-PULL");

            connectorClient.waitTransferInState(participants.consumer().contextId(), transferProcessId, TransferProcessStates.TERMINATED);
        }

        @Test
        void negotiation_isApprovedAutomatically_withCredentialHelperFunction(ManagementApiClientV5 connectorClient,
                                                                               Participants participants) {

            var leftOperand = "https://w3id.org/example/approval/MembershipCredential";
            var expression = "ctx.agent.claims.vc.valid().withType('MembershipCredential').hasClaim('status', 'active')";

            var expr = new CelExpressionDto(leftOperand, expression, Set.of(APPROVAL_SCOPE), "membership approval expression");
            connectorClient.expressions().createExpression(expr);

            var providerAddress = participants.provider().getProtocolEndpoint();
            var policy = new PolicyDto(List.of(new PermissionDto(new AtomicConstraintDto(leftOperand, "eq", "active"))));

            var assetId = setup(connectorClient, participants.provider(), policy);
            var negotiationId = connectorClient.initContractNegotiation(participants.consumer().contextId(), participants.consumer().profile(), assetId, providerAddress, participants.provider().id());

            // the claims stored on the provider negotiation contain the consumer's MembershipCredential, so no manual approval is needed
            connectorClient.waitForContractNegotiationState(participants.consumer().contextId(), negotiationId, ContractNegotiationStates.FINALIZED.name());
        }

        @Test
        void negotiation_isHeld_withMissingCredentialHelperFunction_andApproved(ManagementApiClientV5 connectorClient,
                                                                                Participants participants) {

            var leftOperand = "https://w3id.org/example/approval/DataAccessCredential";
            var expression = "ctx.agent.claims.vc.hasCredential('DataAccessCredential')";

            var expr = new CelExpressionDto(leftOperand, expression, Set.of(APPROVAL_SCOPE), "data access approval expression");
            connectorClient.expressions().createExpression(expr);

            var providerAddress = participants.provider().getProtocolEndpoint();
            var policy = new PolicyDto(List.of(new PermissionDto(new AtomicConstraintDto(leftOperand, "eq", "active"))));

            var assetId = setup(connectorClient, participants.provider(), policy);
            var negotiationId = connectorClient.initContractNegotiation(participants.consumer().contextId(), participants.consumer().profile(), assetId, providerAddress, participants.provider().id());

            // the consumer has no DataAccessCredential, so the provider negotiation is held for manual approval
            var providerNegotiation = await().until(() -> connectorClient.negotiations()
                            .search(participants.provider().contextId(), new QuerySpectDto(List.of(new CriterionDto("correlationId", "=", negotiationId))))
                            .stream().filter(ContractNegotiationDto::isPending).findFirst(),
                    Optional::isPresent).orElseThrow();
            assertThat(providerNegotiation.getState()).isEqualTo(ContractNegotiationStates.REQUESTED.name());

            connectorClient.negotiations().approve(participants.provider().contextId(), providerNegotiation.getId());

            connectorClient.waitForContractNegotiationState(participants.consumer().contextId(), negotiationId, ContractNegotiationStates.FINALIZED.name());
            connectorClient.waitForContractNegotiationState(participants.provider().contextId(), providerNegotiation.getId(), ContractNegotiationStates.FINALIZED.name());
        }


    }

    @Nested
    @PostgresqlIntegrationTest
    class PostgresDcp extends DcpTransferPullEndToEndTestBase {

        @RegisterExtension
        @Order(0)
        static final IssuerServiceEndToEndExtension ISSUER_SERVICE = new IssuerServiceEndToEndExtension();

        @RegisterExtension
        @Order(0)
        static final CredentialServiceEndToEndExtension CREDENTIAL_SERVICE = new CredentialServiceEndToEndExtension();

        @RegisterExtension
        @Order(0)
        static final Oauth2Extension OAUTH_SERVER = new Oauth2Extension();

        @Order(0)
        @RegisterExtension
        static final OauthServerEndToEndExtension AUTH_SERVER_EXTENSION = OauthServerEndToEndExtension.Builder.newInstance().build();

        @Order(0)
        @RegisterExtension
        static final NatsEndToEndExtension NATS_EXTENSION = new NatsEndToEndExtension();
        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();

        @Order(0)
        @RegisterExtension
        static final VaultEndToEndExtension VAULT_END_TO_END_EXTENSION = new VaultEndToEndExtension();

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback SETUP = context -> {
            POSTGRESQL_EXTENSION.createDatabase(Runtimes.ControlPlane.NAME.toLowerCase());
        };

        @Order(2)
        @RegisterExtension
        static final RuntimeExtension CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.VIRTUAL_MODULES)
                .modules(Runtimes.ControlPlane.VIRTUAL_SQL_MODULES)
                .modules(Runtimes.ControlPlane.VIRTUAL_DCP_MODULES)
                .modules(Runtimes.ControlPlane.VIRTUAL_NATS_MODULES)
                .modules(":extensions:common:vault:vault-hashicorp")
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(PostgresDcp::runtimeConfiguration)
                .configurationProvider(() -> ConfigFactory.fromMap(Map.ofEntries(
                        entry("edc.iam.did.web.use.https", "false"),
                        entry("edc.iam.trustedissuer.issuer.id", ISSUER_SERVICE.getIssuerService().getDid()),
                        entry("edc.iam.trustedissuer.issuer.supportedtypes", "[\"*\"]")
                )))
                .configurationProvider(VirtualTransferEndToEndTest::config)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(Runtimes.ControlPlane.NAME.toLowerCase()))
                .configurationProvider(NATS_EXTENSION::configFor)
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .configurationProvider(VAULT_END_TO_END_EXTENSION::configFor)
                .paramProvider(ManagementApiClientV5.class, (ctx) -> ManagementApiClientV5.forContext(ctx, AUTH_SERVER_EXTENSION.getAuthServer()))
                .paramProvider(Participants.class, (ctx) -> participants(ctx, CREDENTIAL_SERVICE))
                .build();

        @RegisterExtension
        @Order(1)
        static final RuntimeExtension PROVIDER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_DP)
                .modules(Runtimes.SignalingDataPlane.MODULES)
                .endpoints(Runtimes.SignalingDataPlane.ENDPOINTS.build())
                .configurationProvider(() -> Runtimes.SignalingDataPlane.config(OAUTH_SERVER.jwksUri()))
                .paramProvider(DataPlaneSignalingTestClient.class, DataPlaneSignalingTestClient::new)
                .build();

        @RegisterExtension
        @Order(1)
        static final RuntimeExtension CONSUMER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_DP)
                .modules(Runtimes.SignalingDataPlane.MODULES)
                .endpoints(Runtimes.SignalingDataPlane.ENDPOINTS.build())
                .configurationProvider(() -> Runtimes.SignalingDataPlane.config(OAUTH_SERVER.jwksUri()))
                .paramProvider(DataPlaneSignalingTestClient.class, DataPlaneSignalingTestClient::new)
                .build();

        private static Config runtimeConfiguration() {
            return ConfigFactory.fromMap(new HashMap<>() {
                {
                    put("edc.iam.dcp.scopes.membership.id", "membership-scope");
                    put("edc.iam.dcp.scopes.membership.type", "DEFAULT");
                    put("edc.iam.dcp.scopes.membership.value", "org.eclipse.dspace.dcp.vc.type:MembershipCredential:read");
                    put("edc.iam.dcp.scopes.data-access.id", "data-access-scope");
                    put("edc.iam.dcp.scopes.data-access.type", "POLICY");
                    put("edc.iam.dcp.scopes.data-access.value", "org.eclipse.dspace.dcp.vc.type:DataAccessCredential:read");
                    put("edc.iam.dcp.scopes.data-access.prefix.mapping", "https://w3id.org/example/credentials/DataAccessCredential");
                    put("edc.negotiation.approval.enabled", "true");
                    put("edc.policy.monitor.period", "PT1S");
                }
            });
        }
    }

}
