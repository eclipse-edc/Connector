/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
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
import de.fraunhofer.iais.eis.DescriptionResponseMessage;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessage;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessageImpl;
import de.fraunhofer.iais.eis.PermissionBuilder;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.RequestInProcessMessageImpl;
import de.fraunhofer.iais.eis.ResponseMessage;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.IdsMultipartRemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message.MultipartDescriptionResponse;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message.MultipartMessageProcessedResponse;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message.MultipartRequestInProcessResponse;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.MultipartArtifactRequestSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.MultipartCatalogDescriptionRequestSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.MultipartContractAgreementSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.MultipartContractOfferSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.MultipartContractRejectionSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.MultipartDescriptionRequestSender;
import org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil;
import org.eclipse.dataspaceconnector.ids.spi.Protocols;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.testOkHttpClient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultipartDispatcherIntegrationTest extends AbstractMultipartDispatcherIntegrationTest {
    private static final String CONNECTOR_ID = UUID.randomUUID().toString();
    private IdsTransformerRegistry transformerRegistry;
    private IdsMultipartRemoteMessageDispatcher multipartDispatcher;

    @BeforeEach
    void init() {
        Monitor monitor = mock(Monitor.class);

        transformerRegistry = mock(IdsTransformerRegistry.class);

        Vault vault = mock(Vault.class);
        var httpClient = testOkHttpClient();

        var idsWebhookAddress = "http://webhook";
        var idsApiPath = "/api";

        multipartDispatcher = new IdsMultipartRemoteMessageDispatcher();
        multipartDispatcher.register(new MultipartDescriptionRequestSender(CONNECTOR_ID, httpClient, OBJECT_MAPPER, monitor, identityService, transformerRegistry));
        multipartDispatcher.register(new MultipartArtifactRequestSender(CONNECTOR_ID, httpClient, OBJECT_MAPPER, monitor, vault, identityService, transformerRegistry, idsWebhookAddress, idsApiPath));
        multipartDispatcher.register(new MultipartContractOfferSender(CONNECTOR_ID, httpClient, OBJECT_MAPPER, monitor, identityService, transformerRegistry, idsWebhookAddress, idsApiPath));
        multipartDispatcher.register(new MultipartContractAgreementSender(CONNECTOR_ID, httpClient, OBJECT_MAPPER, monitor, identityService, transformerRegistry, idsWebhookAddress, idsApiPath));
        multipartDispatcher.register(new MultipartContractRejectionSender(CONNECTOR_ID, httpClient, OBJECT_MAPPER, monitor, identityService, transformerRegistry));
        multipartDispatcher.register(new MultipartCatalogDescriptionRequestSender(CONNECTOR_ID, httpClient, OBJECT_MAPPER, monitor, identityService, transformerRegistry));
    }

    @Test
    void testSendDescriptionRequestMessage() throws Exception {
        var request = MetadataRequest.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(Protocols.IDS_MULTIPART)
                .build();

        var result = multipartDispatcher.send(MultipartDescriptionResponse.class, request, () -> null).get();

        assertThat(result).isNotNull();
        assertThat(result.getHeader()).isNotNull();
        assertThat(result.getHeader()).isInstanceOf(DescriptionResponseMessage.class);
        assertThat(result.getPayload()).isNotNull();
        assertThat(result.getPayload()).isInstanceOf(BaseConnector.class);
    }

    @Test
    void testSendArtifactRequestMessage() throws Exception {
        var asset = Asset.Builder.newInstance().id("1").build();
        addAsset(asset);
        when(transformerRegistry.transform(any(), any()))
                .thenReturn(Result.success(URI.create("urn:artifact:1")));
        when(transformerRegistry.transform(any(), any()))
                .thenReturn(Result.success(URI.create("urn:contract:1")));

        var request = DataRequest.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(Protocols.IDS_MULTIPART)
                .contractId("1")
                .assetId(asset.getId())
                .dataDestination(DataAddress.Builder.newInstance().type("test-type").build())
                .build();

        var result = multipartDispatcher.send(MultipartRequestInProcessResponse.class, request, () -> null).get();

        assertThat(result).isNotNull();
        assertThat(result.getHeader()).isNotNull();

        //TODO revise when handler for ArtifactRequestMessage exists
        assertThat(result.getHeader()).isInstanceOf(ResponseMessage.class);
        assertThat(result.getPayload()).isNull();
        verify(transformerRegistry, times(2)).transform(any(), any());
    }

    @Test
    void testSendContractOfferMessage() throws Exception {
        var contractOffer = ContractOffer.Builder.newInstance().id("id").policy(Policy.Builder.newInstance().build()).build();
        when(transformerRegistry.transform(any(), any()))
                .thenReturn(Result.success(getIdsContractOffer()));

        var request = ContractOfferRequest.Builder.newInstance()
                .type(ContractOfferRequest.Type.COUNTER_OFFER)
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(Protocols.IDS_MULTIPART)
                .contractOffer(contractOffer)
                .correlationId("1")
                .build();

        var result = multipartDispatcher.send(MultipartRequestInProcessResponse.class, request, () -> null).get();

        assertThat(result).isNotNull();
        assertThat(result.getHeader()).isNotNull();

        assertThat(result.getHeader()).isInstanceOf(RequestInProcessMessageImpl.class);
        assertThat(result.getPayload()).isNull();
        verify(transformerRegistry).transform(any(), any());
    }

    @Test
    void testSendContractRequestMessage() throws Exception {
        var contractOffer = ContractOffer.Builder.newInstance().id("id").policy(Policy.Builder.newInstance().build()).build();
        when(transformerRegistry.transform(any(), any()))
                .thenReturn(Result.success(getIdsContractOffer()));

        var request = ContractOfferRequest.Builder.newInstance()
                .type(ContractOfferRequest.Type.INITIAL)
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(Protocols.IDS_MULTIPART)
                .contractOffer(contractOffer)
                .correlationId("1")
                .build();

        var result = multipartDispatcher.send(MultipartRequestInProcessResponse.class, request, () -> null).get();

        assertThat(result).isNotNull();
        assertThat(result.getHeader()).isNotNull();

        // TODO Should be RequestInProcess
        assertThat(result.getHeader()).isInstanceOf(RejectionMessage.class);
        assertThat(result.getPayload()).isNull();
        verify(transformerRegistry).transform(any(), any());
    }

    @Test
    void testSendContractAgreementMessage() throws Exception {
        var contractAgreement = ContractAgreement.Builder.newInstance()
                .id("1:23456").consumerAgentId("consumer").providerAgentId("provider")
                .policy(Policy.Builder.newInstance().build())
                .assetId(UUID.randomUUID().toString())
                .build();

        when(transformerRegistry.transform(any(), eq(de.fraunhofer.iais.eis.ContractAgreement.class)))
                .thenReturn(Result.success(getIdsContractAgreement()));
        when(transformerRegistry.transform(any(), eq(URI.class)))
                .thenReturn(Result.success(URI.create("https://example.com")));

        var request = ContractAgreementRequest.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(Protocols.IDS_MULTIPART)
                .contractAgreement(contractAgreement)
                .correlationId("1")
                .build();

        var result = multipartDispatcher.send(MultipartMessageProcessedResponse.class, request, () -> null).get();

        assertThat(result).isNotNull();
        assertThat(result.getHeader()).isNotNull();

        assertThat(result.getHeader()).isInstanceOf(MessageProcessedNotificationMessageImpl.class);
        assertThat(result.getPayload()).isNull();
        verify(transformerRegistry, times(2)).transform(any(), any());
    }

    @Test
    void testSendContractRejectionMessage() throws Exception {
        var rejection = ContractRejection.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(Protocols.IDS_MULTIPART)
                .rejectionReason("Modified policy in contract offer.")
                .correlationId(UUID.randomUUID().toString())
                .build();

        var result = multipartDispatcher.send(MultipartMessageProcessedResponse.class, rejection, () -> null).get();

        assertThat(result).isNotNull();
        assertThat(result.getHeader()).isNotNull();

        assertThat(result.getHeader()).isInstanceOf(MessageProcessedNotificationMessage.class);
        assertThat(result.getPayload()).isNull();
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
        return new ContractOfferBuilder()
                ._consumer_(URI.create("consumer"))
                ._provider_(URI.create("provider"))
                ._permission_(new PermissionBuilder()
                        ._action_(Action.USE)
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
