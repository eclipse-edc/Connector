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
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participants;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.ManagementApiClientV5;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.AssetDto;
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
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Diagnostic E2E test for GH issue #5610 ("Externalize state machine retrial decision").
 * <p>
 * In the virtual, NATS-driven control plane a state transition is executed by a task subscriber that redelivers the
 * message on transient failures. The classic {@code RetryProcessor} decides when to give up based on the entity's
 * {@code stateCount} vs {@code edc.negotiation.send.retry.limit}, while the NATS subscriber has its own, independent
 * ceiling ({@code edc.nats.cn.subscriber.max-retries}). This test verifies that the entity {@code stateCount}-based
 * retry limit actually drives a negotiation to {@code TERMINATED} when the counter-party keeps failing over NATS.
 * <p>
 * A retryable failure is produced by pointing the consumer at a fake provider that always answers HTTP 503 (the DSP
 * dispatcher maps 5xx to {@code ERROR_RETRY}; 4xx and connection errors map to {@code FATAL_ERROR}). The NATS
 * {@code max-retries} is set high and {@code send.retry.limit} low so the {@code stateCount} ceiling is the operative
 * one and the task-redelivery ceiling cannot mask the outcome.
 */
@PostgresqlIntegrationTest
class VirtualNegotiationRetryEndToEndTest {

    static final String PROVIDER_CONTEXT = "provider";
    static final String CONSUMER_CONTEXT = "consumer";
    static final String PROVIDER_ID = "provider-id";
    static final String CONSUMER_ID = "consumer-id";

    // value of edc.negotiation.send.retry.limit applied to the runtime below
    static final int NEGOTIATION_SEND_RETRY_LIMIT = 5;

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
    static final WireMockExtension FAKE_PROVIDER = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Order(1)
    @RegisterExtension
    static final BeforeAllCallback SETUP = context ->
            POSTGRESQL_EXTENSION.createDatabase(Runtimes.ControlPlane.NAME.toLowerCase());

    @Order(2)
    @RegisterExtension
    static final RuntimeExtension CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
            .name(Runtimes.ControlPlane.NAME)
            .modules(Runtimes.ControlPlane.VIRTUAL_MODULES)
            .modules(Runtimes.ControlPlane.VIRTUAL_SQL_MODULES)
            .modules(Runtimes.ControlPlane.VIRTUAL_NATS_MODULES)
            .modules(Runtimes.ControlPlane.IAM_MOCK)
            .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
            .configurationProvider(VirtualNegotiationRetryEndToEndTest::config)
            .configurationProvider(VirtualNegotiationRetryEndToEndTest::retryConfig)
            .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(Runtimes.ControlPlane.NAME.toLowerCase()))
            .configurationProvider(NATS_EXTENSION::configFor)
            .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
            .paramProvider(ManagementApiClientV5.class, ctx -> ManagementApiClientV5.forContext(ctx, AUTH_SERVER_EXTENSION.getAuthServer()))
            .paramProvider(Participants.class, VirtualNegotiationRetryEndToEndTest::participants)
            .build();

    @BeforeAll
    static void beforeAll(ManagementApiClientV5 connectorClient, Participants participants) {
        connectorClient.createParticipant(participants.consumer().contextId(), participants.consumer().id(), participants.consumer().config());
        connectorClient.createParticipant(participants.provider().contextId(), participants.provider().id(), participants.provider().config());
    }

    private static Config config() {
        return ConfigFactory.fromMap(Map.of(
                "edc.iam.oauth2.jwks.url", "https://example.com/jwks",
                "edc.iam.oauth2.issuer", "test-issuer",
                "web.http.protocol.virtual", "true",
                "edc.dataspace.enable.profiles.all", "true"
        ));
    }

    private static Config retryConfig() {
        return ConfigFactory.fromMap(Map.of(
                // do not retry http requests
                "edc.core.retry.retries.max", "0",
                // make the entity stateCount ceiling the operative one (default is 7)...
                "edc.negotiation.send.retry.limit", String.valueOf(NEGOTIATION_SEND_RETRY_LIMIT),
                "edc.negotiation.send.retry.base-delay.ms", "100",
                "edc.negotiation.state-machine.iteration-wait-millis", "50"
        ));
    }

    private static Participants participants(ComponentRuntimeContext ctx) {
        var protocolEndpoint = ctx.getEndpoint("protocol");
        var signalingEndpoint = ctx.getEndpoint("signaling");
        return new Participants(
                new Participants.Participant(PROVIDER_CONTEXT, PROVIDER_ID, protocolEndpoint, signalingEndpoint),
                new Participants.Participant(CONSUMER_CONTEXT, CONSUMER_ID, protocolEndpoint, signalingEndpoint)
        );
    }

    private static String setup(ManagementApiClientV5 connectorClient, Participants.Participant provider) {
        var policy = new PolicyDto(List.of(new PermissionDto()));
        var policyDef = new PolicyDefinitionDto(policy);
        return connectorClient.setupResources(provider.contextId(), new AssetDto(), policyDef, policyDef);
    }

    @Test
    void contractNegotiation_shouldTerminate_afterRetryLimit_whenCounterPartyKeepsFailing(ManagementApiClientV5 connectorClient, Participants participants) {
        // the fake provider answers 503 to every DSP request -> ERROR_RETRY (retryable) on the consumer side
        FAKE_PROVIDER.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(503).withBody("{}")));

        var consumer = participants.consumer();
        var provider = participants.provider();

        // set up a real offer on the provider so we have a valid offer to send...
        var assetId = setup(connectorClient, provider);
        var dataset = connectorClient.fetchDataset(consumer.contextId(), consumer.profile(), assetId, provider.getProtocolEndpoint(), provider.id());
        var offer = dataset.offers().stream().findFirst().orElseThrow();

        // ...but direct the negotiation at the failing (503) endpoint so processRequesting keeps failing
        var negotiationId = connectorClient.initContractNegotiation(consumer.contextId(), consumer.profile(), assetId, offer, FAKE_PROVIDER.baseUrl(), provider.id());

        // the stateCount-based retry limit must eventually drive the negotiation to TERMINATED over NATS
        connectorClient.waitForContractNegotiationState(consumer.contextId(), negotiationId, ContractNegotiationStates.TERMINATED.name());

        assertThat(connectorClient.getNegotiationError(consumer.contextId(), negotiationId))
                .contains("Failed to request contract to provider");

        // the request is dispatched once per attempt; RetryProcessor terminates when stateCount (starts at 1 in
        // REQUESTING) exceeds the limit, so the counter-party is hit exactly (limit + 1) times. The catalog fetch
        // above targets the real provider, not this stub, so every recorded request is a ContractRequestMessage.
        FAKE_PROVIDER.verify(NEGOTIATION_SEND_RETRY_LIMIT + 1, anyRequestedFor(anyUrl()));
    }
}
