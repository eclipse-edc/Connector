/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.client;

import de.fraunhofer.iais.eis.Action;
import de.fraunhofer.iais.eis.BaseConnector;
import de.fraunhofer.iais.eis.ContractAgreementBuilder;
import de.fraunhofer.iais.eis.ContractOfferBuilder;
import de.fraunhofer.iais.eis.PermissionBuilder;
import org.eclipse.dataspaceconnector.common.util.junit.annotations.ComponentTest;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.IdsMultipartRemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.IdsMultipartSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.SenderDelegateContext;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.type.MultipartArtifactRequestSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.type.MultipartCatalogDescriptionRequestSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.type.MultipartContractAgreementSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.type.MultipartContractOfferSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.type.MultipartContractRejectionSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.type.MultipartDescriptionRequestSender;
import org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.ids.spi.types.MessageProtocol;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreementRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractRejection;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.MetadataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.testOkHttpClient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class MultipartDispatcherIntegrationTest extends AbstractMultipartDispatcherIntegrationTest {
    private static final String CONNECTOR_ID = UUID.randomUUID().toString();
    private IdsTransformerRegistry transformerRegistry;
    private IdsMultipartRemoteMessageDispatcher dispatcher;

    @BeforeEach
    void init() {
        Monitor monitor = mock(Monitor.class);

        transformerRegistry = mock(IdsTransformerRegistry.class);

        Vault vault = mock(Vault.class);
        var httpClient = testOkHttpClient();

        var idsWebhookAddress = "http://webhook/api";
    
        var senderContext = new SenderDelegateContext(URI.create(CONNECTOR_ID), objectMapper, transformerRegistry, idsWebhookAddress);

        var sender = new IdsMultipartSender(monitor, httpClient, identityService, objectMapper);
        dispatcher = new IdsMultipartRemoteMessageDispatcher(sender);
        dispatcher.register(new MultipartArtifactRequestSender(senderContext, vault));
        dispatcher.register(new MultipartDescriptionRequestSender(senderContext));
        dispatcher.register(new MultipartContractOfferSender(senderContext));
        dispatcher.register(new MultipartContractAgreementSender(senderContext));
        dispatcher.register(new MultipartContractRejectionSender(senderContext));
        dispatcher.register(new MultipartCatalogDescriptionRequestSender(senderContext));
    }

    @Test
    void testSendDescriptionRequestMessage() throws Exception {
        var request = MetadataRequest.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(MessageProtocol.IDS_MULTIPART)
                .build();

        var result = dispatcher.send(BaseConnector.class, request, () -> null).get();

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(BaseConnector.class);
    }

    @Test
    void testSendArtifactRequestMessage() {
        var asset = Asset.Builder.newInstance().id("1").build();
        addAsset(asset);
        when(negotiationStore.findContractAgreement(any())).thenReturn(ContractAgreement.Builder.newInstance()
                .providerAgentId("provider")
                .consumerAgentId("consumer")
                .assetId("1")
                .policy(Policy.Builder.newInstance().build())
                .contractSigningDate(Instant.now().getEpochSecond())
                .contractStartDate(Instant.now().getEpochSecond())
                .contractEndDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond())
                .id("1:2").build());

        var request = DataRequest.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(MessageProtocol.IDS_MULTIPART)
                .contractId("1")
                .assetId(asset.getId())
                .dataDestination(DataAddress.Builder.newInstance().type("test-type").build())
                .build();

        assertThatCode(() -> dispatcher.send(null, request, () -> null)).doesNotThrowAnyException();
    }

    @Test
    void testSendContractOfferMessage() {
        var contractOffer = ContractOffer.Builder.newInstance().id("id").policy(Policy.Builder.newInstance().build()).build();
        when(transformerRegistry.transform(any(), any()))
                .thenReturn(Result.success(getIdsContractOffer()));

        var request = ContractOfferRequest.Builder.newInstance()
                .type(ContractOfferRequest.Type.COUNTER_OFFER)
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(MessageProtocol.IDS_MULTIPART)
                .contractOffer(contractOffer)
                .correlationId("1")
                .build();

        assertThatCode(() -> dispatcher.send(null, request, () -> null)).doesNotThrowAnyException();

        verify(transformerRegistry).transform(any(), any());
    }

    @Test
    void testSendContractRequestMessage() {
        var policy = Policy.Builder.newInstance().build();
        var contractOffer = ContractOffer.Builder.newInstance().id("id").policy(policy).build();

        addAsset(Asset.Builder.newInstance().id("1").build());

        when(transformerRegistry.transform(any(), eq(de.fraunhofer.iais.eis.ContractOffer.class))).thenReturn(Result.success(getIdsContractOffer()));

        var request = ContractOfferRequest.Builder.newInstance()
                .type(ContractOfferRequest.Type.INITIAL)
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(MessageProtocol.IDS_MULTIPART)
                .contractOffer(contractOffer)
                .correlationId("1")
                .build();

        assertThatCode(() -> dispatcher.send(null, request, () -> null)).doesNotThrowAnyException();

        verify(transformerRegistry).transform(any(), any());
    }

    @Test
    void testSendContractAgreementMessage() {
        var contractAgreement = ContractAgreement.Builder.newInstance()
                .id("1:23456").consumerAgentId("consumer").providerAgentId("provider")
                .policy(Policy.Builder.newInstance().build())
                .assetId(UUID.randomUUID().toString())
                .build();

        when(transformerRegistry.transform(any(), eq(de.fraunhofer.iais.eis.ContractAgreement.class)))
                .thenReturn(Result.success(getIdsContractAgreement()));

        var request = ContractAgreementRequest.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(MessageProtocol.IDS_MULTIPART)
                .contractAgreement(contractAgreement)
                .correlationId("1")
                .policy(Policy.Builder.newInstance().build())
                .build();

        assertThatCode(() -> dispatcher.send(null, request, () -> null)).doesNotThrowAnyException();

        verify(transformerRegistry, times(1)).transform(any(), any());
    }

    @Test
    void testSendContractRejectionMessage() {
        var rejection = ContractRejection.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(MessageProtocol.IDS_MULTIPART)
                .rejectionReason("Modified policy in contract offer.")
                .correlationId(UUID.randomUUID().toString())
                .build();

        assertThatCode(() -> dispatcher.send(null, rejection, () -> null)).doesNotThrowAnyException();
    }

    @Override
    protected Map<String, String> getSystemProperties() {
        return new HashMap<>() {
            {
                put("web.http.port", String.valueOf(getPort()));
                put("web.http.path", "/api");
                put("web.http.ids.port", String.valueOf(getIdsPort()));
                put("web.http.ids.path", "/api/v1/ids");
                put("edc.ids.id", "urn:connector:" + CONNECTOR_ID);
                put("ids.webhook.address", "http://webhook");
            }
        };
    }

    private de.fraunhofer.iais.eis.ContractOffer getIdsContractOffer() {
        return new ContractOfferBuilder(URI.create("urn:contractoffer:1"))
                ._consumer_(URI.create("consumer"))
                ._provider_(URI.create("provider"))
                ._permission_(new PermissionBuilder()
                        ._action_(Action.USE)
                        ._target_(URI.create("urn:artifact:1"))
                        .build())
                .build();
    }

    private de.fraunhofer.iais.eis.ContractAgreement getIdsContractAgreement() {
        return new ContractAgreementBuilder(URI.create("urn:contractagreement:1"))
                ._consumer_(URI.create("consumer"))
                ._provider_(URI.create("provider"))
                ._contractDate_(CalendarUtil.gregorianNow())
                ._contractEnd_(CalendarUtil.gregorianNow())
                ._contractStart_(CalendarUtil.gregorianNow())
                ._permission_(new PermissionBuilder()
                        ._action_(Action.USE)
                        .build())
                .build();
    }
}
