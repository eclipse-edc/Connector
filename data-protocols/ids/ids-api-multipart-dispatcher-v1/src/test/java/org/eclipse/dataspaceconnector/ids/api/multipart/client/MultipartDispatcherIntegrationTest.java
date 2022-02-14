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
import de.fraunhofer.iais.eis.BaseConnectorBuilder;
import de.fraunhofer.iais.eis.Connector;
import de.fraunhofer.iais.eis.ContractAgreementBuilder;
import de.fraunhofer.iais.eis.ContractOfferBuilder;
import de.fraunhofer.iais.eis.DescriptionResponseMessage;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessage;
import de.fraunhofer.iais.eis.PermissionBuilder;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.RequestInProcessMessage;
import de.fraunhofer.iais.eis.ResponseMessage;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message.MultipartDescriptionResponse;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message.MultipartMessageProcessedResponse;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message.MultipartRequestInProcessResponse;
import org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil;
import org.eclipse.dataspaceconnector.ids.spi.Protocols;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.result.Result;
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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultipartDispatcherIntegrationTest extends AbstractMultipartDispatcherIntegrationTest {
    private static final String CONNECTOR_ID = UUID.randomUUID().toString();
    private final TransformerRegistry transformerRegistry = mock(TransformerRegistry.class);

    @BeforeEach
    void init(EdcExtension extension) {
        extension.registerServiceMock(TransformerRegistry.class, transformerRegistry);
    }

    @Test
    void testSendDescriptionRequestMessage(RemoteMessageDispatcherRegistry dispatcher) {
        var request = MetadataRequest.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(Protocols.IDS_MULTIPART)
                .build();

        when(transformerRegistry.transform(any(), eq(Connector.class)))
                .thenReturn(Result.success(new BaseConnectorBuilder().build()));

        var future = dispatcher.send(MultipartDescriptionResponse.class, request, () -> null);
        var assertion = assertThat(future).succeedsWithin(10, SECONDS).isNotNull();
        assertion.extracting(MultipartDescriptionResponse::getHeader).isInstanceOf(DescriptionResponseMessage.class);
        assertion.extracting(MultipartDescriptionResponse::getPayload).isInstanceOf(BaseConnector.class);
    }

    @Test
    void testSendArtifactRequestMessage(RemoteMessageDispatcherRegistry dispatcher) {
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

        var future = dispatcher.send(MultipartRequestInProcessResponse.class, request, () -> null);

        var assertion = assertThat(future).succeedsWithin(10, SECONDS).isNotNull();
        //TODO revise when handler for ArtifactRequestMessage exists
        assertion.extracting(MultipartRequestInProcessResponse::getHeader).isInstanceOf(ResponseMessage.class);
        assertion.extracting(MultipartRequestInProcessResponse::getPayload).isNull();
        verify(transformerRegistry, times(2)).transform(any(), any());
    }

    @Test
    void testSendContractOfferMessage(RemoteMessageDispatcherRegistry dispatcher) {
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

        var future = dispatcher.send(MultipartRequestInProcessResponse.class, request, () -> null);

        var assertion = assertThat(future).succeedsWithin(10, SECONDS).isNotNull();
        assertion.extracting(MultipartRequestInProcessResponse::getHeader).isInstanceOf(RequestInProcessMessage.class);
        assertion.extracting(MultipartRequestInProcessResponse::getPayload).isNull();
        verify(transformerRegistry).transform(any(), any());
    }

    @Test
    void testSendContractRequestMessage(RemoteMessageDispatcherRegistry dispatcher) {
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

        var future = dispatcher.send(MultipartRequestInProcessResponse.class, request, () -> null);

        var assertion = assertThat(future).succeedsWithin(10, SECONDS).isNotNull();
        assertion.extracting(MultipartRequestInProcessResponse::getHeader).isInstanceOf(RejectionMessage.class);
        assertion.extracting(MultipartRequestInProcessResponse::getPayload).isNull();
        verify(transformerRegistry).transform(any(), any());
    }

    @Test
    void testSendContractAgreementMessage(RemoteMessageDispatcherRegistry dispatcher) {
        var contractAgreement = ContractAgreement.Builder.newInstance()
                .id("1:23456").consumerAgentId("consumer").providerAgentId("provider")
                .policy(Policy.Builder.newInstance().build())
                .asset(Asset.Builder.newInstance().build())
                .build();
        when(transformerRegistry.transform(any(), eq(de.fraunhofer.iais.eis.ContractAgreement.class)))
                .thenReturn(Result.success(getIdsContractAgreement()));
        when(transformerRegistry.transform(any(), eq(ContractAgreement.class)))
                .thenReturn(Result.success(contractAgreement));
        when(transformerRegistry.transform(any(), eq(URI.class)))
                .thenReturn(Result.success(URI.create("https://example.com")));

        var request = ContractAgreementRequest.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(Protocols.IDS_MULTIPART)
                .contractAgreement(contractAgreement)
                .correlationId("1")
                .build();

        var future = dispatcher.send(MultipartMessageProcessedResponse.class, request, () -> null);

        var assertion = assertThat(future).succeedsWithin(10, SECONDS).isNotNull();
        assertion.extracting(MultipartMessageProcessedResponse::getHeader).isInstanceOf(MessageProcessedNotificationMessage.class);
        assertion.extracting(MultipartMessageProcessedResponse::getPayload).isNull();
    }

    @Test
    void testSendContractRejectionMessage(RemoteMessageDispatcherRegistry dispatcher) {
        var rejection = ContractRejection.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(Protocols.IDS_MULTIPART)
                .rejectionReason("Modified policy in contract offer.")
                .correlationId(UUID.randomUUID().toString())
                .build();

        var future = dispatcher.send(MultipartMessageProcessedResponse.class, rejection, () -> null);

        var assertion = assertThat(future).succeedsWithin(10, SECONDS).isNotNull();
        assertion.extracting(MultipartMessageProcessedResponse::getHeader).isInstanceOf(MessageProcessedNotificationMessage.class);
        assertion.extracting(MultipartMessageProcessedResponse::getPayload).isNull();
    }

    @Override
    protected Map<String, String> getConfigurationProperties() {
        return new HashMap<>() {
            {
                put("web.http.port", String.valueOf(getPort()));
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
