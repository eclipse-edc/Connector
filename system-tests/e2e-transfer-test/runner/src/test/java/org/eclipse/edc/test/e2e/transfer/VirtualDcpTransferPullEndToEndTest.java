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
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.AssetDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.AtomicConstraintDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.CelExpressionDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.DataAddressDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.PermissionDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.PolicyDefinitionDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.PolicyDto;
import org.eclipse.edc.iam.decentralizedclaims.spi.credentialservice.CredentialService;
import org.eclipse.edc.iam.decentralizedclaims.spi.credentialservice.CredentialServiceEndToEndExtension;
import org.eclipse.edc.iam.decentralizedclaims.spi.issuerservice.IssuerService;
import org.eclipse.edc.iam.decentralizedclaims.spi.issuerservice.IssuerServiceEndToEndExtension;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.annotations.Runtime;
import org.eclipse.edc.junit.extensions.ComponentRuntimeContext;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.nats.testfixtures.NatsEndToEndExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.Runtimes;
import org.eclipse.edc.test.e2e.dataplane.DataPlaneSignalingClient;
import org.eclipse.edc.test.e2e.signaling.Oauth2Extension;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.CONSUMER_DP;
import static org.eclipse.edc.test.e2e.transfer.VirtualTransferEndToEndTestBase.CONSUMER_CONTEXT;
import static org.eclipse.edc.test.e2e.transfer.VirtualTransferEndToEndTestBase.PROVIDER_CONTEXT;


class VirtualDcpTransferPullEndToEndTest {

    private static Participants participants(ComponentRuntimeContext ctx, CredentialServiceEndToEndExtension ext) {
        var protocolEndpoint = ctx.getEndpoint("protocol");
        var signalingEndpoint = ctx.getEndpoint("signaling");
        var providerDid = ext.didFor(PROVIDER_CONTEXT);
        var providerCfg = ext.dcpConfig(PROVIDER_CONTEXT);
        var consumerDid = ext.didFor(CONSUMER_CONTEXT);
        var consumerCfg = ext.dcpConfig(CONSUMER_CONTEXT);
        return new Participants(
                new Participants.Participant(PROVIDER_CONTEXT, providerDid, protocolEndpoint, signalingEndpoint, providerCfg.getEntries()),
                new Participants.Participant(CONSUMER_CONTEXT, consumerDid, protocolEndpoint, signalingEndpoint, consumerCfg.getEntries())
        );
    }

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class DcpTransferPullEndToEndTestBase extends VirtualTransferEndToEndTestBase {

        /**
         * Set up the test environment by creating one issuer, two participants in their
         * respective Identity Hubs, and issuing a MembershipCredential credential for each participant.
         */
        @BeforeAll
        static void setup(IssuerService issuer,
                          CredentialService credentialService,
                          Participants participants,
                          @Runtime(Runtimes.ControlPlane.NAME) Vault vault,
                          TrustedIssuerRegistry trustedIssuerRegistry) {

            trustedIssuerRegistry.register(new Issuer(issuer.getDid(), Map.of()), "*");

            var consumerContextId = participants.consumer().contextId();
            var providerContextId = participants.provider().contextId();
            vault.storeSecret(consumerContextId, "%s-alias".formatted(consumerContextId), "%s-sts-secret".formatted(consumerContextId));
            vault.storeSecret(providerContextId, "%s-alias".formatted(providerContextId), "%s-sts-secret".formatted(providerContextId));

            credentialService.addParticipant(participants.consumer().contextId());
            credentialService.addParticipant(participants.provider().contextId());

            var consumerMembershipCredential = issuer.issueCredential(participants.consumer().id(), "MembershipCredential", Map.of("status", "active"));
            var providerMembershipCredential = issuer.issueCredential(participants.provider().id(), "MembershipCredential", Map.of("status", "active"));

            credentialService.storeCredential(participants.consumer().contextId(), consumerMembershipCredential);
            credentialService.storeCredential(participants.provider().contextId(), providerMembershipCredential);
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
            var transferProcessId = connectorClient.startTransfer(participants.consumer().contextId(), participants.provider().contextId(), providerAddress, participants.provider().id(), assetId, "NonFinite-PULL");

            var consumerTransfer = connectorClient.transfers().getTransferProcess(participants.consumer().contextId(), transferProcessId);
            var providerTransfer = connectorClient.transfers().getTransferProcess(participants.provider().contextId(), consumerTransfer.getCorrelationId());

            assertThat(consumerTransfer.getState()).isEqualTo(providerTransfer.getState());

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
            var negotiationId = connectorClient.initContractNegotiation(participants.consumer().contextId(), assetId, providerAddress, participants.provider().id());

            connectorClient.waitForContractNegotiationState(participants.consumer().contextId(), negotiationId, ContractNegotiationStates.TERMINATED.name());
            var error = connectorClient.getNegotiationError(participants.consumer().contextId(), negotiationId);

            assertThat(error).isNotNull().contains("Unauthorized");

        }

        private String setup(ManagementApiClientV5 connectorClient, Participants.Participant provider, PolicyDto policy) {
            var asset = new AssetDto(new DataAddressDto("HttpData"));
            var policyDef = new PolicyDefinitionDto(policy);

            return connectorClient.setupResources(provider.contextId(), asset, policyDef, policyDef);
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

        // DPS registration
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
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .modules(Runtimes.ControlPlane.DCP_MODULES)
                .modules(Runtimes.ControlPlane.VIRTUAL_DCP_MODULES)
                .modules(Runtimes.ControlPlane.VIRTUAL_SQL_MODULES)
                .modules(Runtimes.ControlPlane.VIRTUAL_NATS_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(PostgresDcp::runtimeConfiguration)
                .configurationProvider(() -> ConfigFactory.fromMap(Map.of("edc.iam.did.web.use.https", "false")))
                .configurationProvider(VirtualTransferEndToEndTest::config)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(Runtimes.ControlPlane.NAME.toLowerCase()))
                .configurationProvider(NATS_EXTENSION::configFor)
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementApiClientV5.class, (ctx) -> ManagementApiClientV5.forContext(ctx, AUTH_SERVER_EXTENSION.getAuthServer()))
                .paramProvider(Participants.class, (ctx) -> participants(ctx, CREDENTIAL_SERVICE))
                .build();

        @RegisterExtension
        @Order(1)
        static final RuntimeExtension PROVIDER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_DP)
                .modules(Runtimes.SignalingDataPlane.MODULES)
                .endpoints(Runtimes.SignalingDataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.SignalingDataPlane::config)
                .paramProvider(DataPlaneSignalingClient.class, DataPlaneSignalingClient::new)
                .build();

        @RegisterExtension
        @Order(1)
        static final RuntimeExtension CONSUMER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_DP)
                .modules(Runtimes.SignalingDataPlane.MODULES)
                .endpoints(Runtimes.SignalingDataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.SignalingDataPlane::config)
                .paramProvider(DataPlaneSignalingClient.class, DataPlaneSignalingClient::new)
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
                    put("edc.iam.dcp.scopes.data-access.prefix-mapping", "https://w3id.org/example/credentials/DataAccessCredential");
                }
            });
        }
    }

}
