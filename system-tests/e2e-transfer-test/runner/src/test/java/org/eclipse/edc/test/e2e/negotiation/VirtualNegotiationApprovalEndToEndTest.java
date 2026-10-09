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

package org.eclipse.edc.test.e2e.negotiation;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.restassured.path.json.JsonPath;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participants;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.ManagementApiClientV5;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.AssetDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.AtomicConstraintDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.CelExpressionDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.PermissionDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.PolicyDefinitionDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.PolicyDto;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeContext;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.nats.testfixtures.NatsEndToEndExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.Runtimes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.ApprovalContractNegotiationPolicyContext.APPROVAL_SCOPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATED;

/**
 * Manual approval of provider contract negotiations in the virtual connector, where negotiations are processed by the
 * task executor.
 */
@PostgresqlIntegrationTest
class VirtualNegotiationApprovalEndToEndTest {

    private static final String PROVIDER_CONTEXT = "provider";
    private static final String CONSUMER_CONTEXT = "consumer";
    private static final String PROVIDER_ID = "provider-id";
    private static final String CONSUMER_ID = "consumer-id";

    @Order(0)
    @RegisterExtension
    static final WireMockExtension CALLBACK_SERVER = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

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
    static final BeforeAllCallback SETUP = context -> POSTGRESQL_EXTENSION.createDatabase(Runtimes.ControlPlane.NAME.toLowerCase());

    @Order(2)
    @RegisterExtension
    static final RuntimeExtension CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
            .name(Runtimes.ControlPlane.NAME)
            .modules(Runtimes.ControlPlane.VIRTUAL_MODULES)
            .modules(Runtimes.ControlPlane.VIRTUAL_SQL_MODULES)
            .modules(Runtimes.ControlPlane.VIRTUAL_NATS_MODULES)
            .modules(Runtimes.ControlPlane.IAM_MOCK)
            .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
            .configurationProvider(VirtualNegotiationApprovalEndToEndTest::config)
            .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(Runtimes.ControlPlane.NAME.toLowerCase()))
            .configurationProvider(NATS_EXTENSION::configFor)
            .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
            .paramProvider(ManagementApiClientV5.class, ctx -> ManagementApiClientV5.forContext(ctx, AUTH_SERVER_EXTENSION.getAuthServer()))
            .paramProvider(Participants.class, VirtualNegotiationApprovalEndToEndTest::participants)
            .build();

    private static Config config() {
        return ConfigFactory.fromMap(new HashMap<>() {
            {
                put("edc.iam.oauth2.jwks.url", "https://example.com/jwks");
                put("edc.iam.oauth2.issuer", "test-issuer");
                put("web.http.protocol.virtual", "true");
                put("edc.dataspace.enable.profiles.all", "true");
                put("edc.negotiation.approval.enabled", "true");
            }
        });
    }

    private static Participants participants(ComponentRuntimeContext ctx) {
        var protocolEndpoint = ctx.getEndpoint("protocol");
        var signalingEndpoint = ctx.getEndpoint("signaling");
        var callbacks = """
                [{"uri": "%s/callbacks", "events": ["contract.negotiation.held"], "transactional": false}]
                """.formatted(CALLBACK_SERVER.baseUrl());
        return new Participants(
                new Participants.Participant(PROVIDER_CONTEXT, PROVIDER_ID, protocolEndpoint, signalingEndpoint, Map.of("edc.callbacks", callbacks)),
                new Participants.Participant(CONSUMER_CONTEXT, CONSUMER_ID, protocolEndpoint, signalingEndpoint)
        );
    }

    @BeforeAll
    static void beforeAll(ManagementApiClientV5 connectorClient, Participants participants) {
        connectorClient.createParticipant(participants.consumer().contextId(), participants.consumer().id(), participants.consumer().config());
        connectorClient.createParticipant(participants.provider().contextId(), participants.provider().id(), participants.provider().config());
    }

    @BeforeEach
    void setUp() {
        // stubs are reset by the WireMock extension before each test
        CALLBACK_SERVER.stubFor(post("/callbacks").willReturn(aResponse().withStatus(200)));
    }

    @Test
    void approve(ManagementApiClientV5 connectorClient, Participants participants) {
        var assetId = setupAssetWithApprovalPolicy(connectorClient, participants);

        var negotiationId = connectorClient.initContractNegotiation(participants.consumer().contextId(), participants.consumer().profile(),
                assetId, participants.provider().getProtocolEndpoint(), participants.provider().id());

        var providerNegotiationId = awaitHeldNegotiation(assetId);
        var providerNegotiation = connectorClient.negotiations().getNegotiation(participants.provider().contextId(), providerNegotiationId);
        assertThat(providerNegotiation.getState()).isEqualTo(REQUESTED.name());
        assertThat(providerNegotiation.isPending()).isTrue();

        connectorClient.negotiations().approve(participants.provider().contextId(), providerNegotiationId);

        connectorClient.waitForContractNegotiationState(participants.consumer().contextId(), negotiationId, FINALIZED.name());
        connectorClient.waitForContractNegotiationState(participants.provider().contextId(), providerNegotiationId, FINALIZED.name());
    }

    @Test
    void reject(ManagementApiClientV5 connectorClient, Participants participants) {
        var assetId = setupAssetWithApprovalPolicy(connectorClient, participants);

        var negotiationId = connectorClient.initContractNegotiation(participants.consumer().contextId(), participants.consumer().profile(),
                assetId, participants.provider().getProtocolEndpoint(), participants.provider().id());

        var providerNegotiationId = awaitHeldNegotiation(assetId);

        connectorClient.negotiations().reject(participants.provider().contextId(), providerNegotiationId);

        connectorClient.waitForContractNegotiationState(participants.consumer().contextId(), negotiationId, TERMINATED.name());
        connectorClient.waitForContractNegotiationState(participants.provider().contextId(), providerNegotiationId, TERMINATED.name());
    }

    @Test
    void terminate(ManagementApiClientV5 connectorClient, Participants participants) {
        var assetId = setupAssetWithApprovalPolicy(connectorClient, participants);

        var negotiationId = connectorClient.initContractNegotiation(participants.consumer().contextId(), participants.consumer().profile(),
                assetId, participants.provider().getProtocolEndpoint(), participants.provider().id());

        var providerNegotiationId = awaitHeldNegotiation(assetId);

        connectorClient.negotiations().terminate(participants.provider().contextId(), providerNegotiationId, "terminated by the provider");

        connectorClient.waitForContractNegotiationState(participants.consumer().contextId(), negotiationId, TERMINATED.name());
        connectorClient.waitForContractNegotiationState(participants.provider().contextId(), providerNegotiationId, TERMINATED.name());
    }

    /**
     * Creates an asset whose contract policy has a constraint evaluated only in the approval scope, which fails for the
     * consumer, so that the provider negotiation is held for manual approval.
     */
    private String setupAssetWithApprovalPolicy(ManagementApiClientV5 connectorClient, Participants participants) {
        var leftOperand = "trustedPartner-" + UUID.randomUUID();
        connectorClient.expressions().createExpression(new CelExpressionDto(leftOperand, "ctx.agent.id == 'a-trusted-partner'",
                Set.of(APPROVAL_SCOPE), "approval expression"));

        var accessPolicy = new PolicyDefinitionDto(new PolicyDto(List.of(new PermissionDto())));
        var contractPolicy = new PolicyDefinitionDto(new PolicyDto(List.of(new PermissionDto(new AtomicConstraintDto(leftOperand, "eq", "true")))));
        return connectorClient.setupResources(participants.provider().contextId(), new AssetDto(), accessPolicy, contractPolicy);
    }

    private String awaitHeldNegotiation(String assetId) {
        return await().until(() -> CALLBACK_SERVER.findAll(postRequestedFor(urlEqualTo("/callbacks"))).stream()
                .map(LoggedRequest::getBodyAsString)
                .filter(body -> body.contains(assetId))
                .map(body -> JsonPath.from(body).getString("payload.contractNegotiationId"))
                .findFirst(), Optional::isPresent).orElseThrow();
    }
}
