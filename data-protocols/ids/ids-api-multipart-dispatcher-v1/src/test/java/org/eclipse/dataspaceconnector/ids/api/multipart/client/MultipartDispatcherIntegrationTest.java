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
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessageImpl;
import de.fraunhofer.iais.eis.PermissionBuilder;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.RejectionMessageImpl;
import de.fraunhofer.iais.eis.RejectionReason;
import de.fraunhofer.iais.eis.RequestInProcessMessageImpl;
import de.fraunhofer.iais.eis.ResponseMessage;
import okhttp3.OkHttpClient;
import org.easymock.EasyMock;
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
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformResult;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreementRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractRejection;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.MetadataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MultipartDispatcherIntegrationTest extends AbstractMultipartDispatcherIntegrationTest {
    private static final String CONNECTOR_ID = UUID.randomUUID().toString();
    private TransformerRegistry transformerRegistry;
    private IdsMultipartRemoteMessageDispatcher multipartDispatcher;

    @Override
    protected Map<String, String> getSystemProperties() {
        return new HashMap<>() {
            {
                put("web.http.port", String.valueOf(getPort()));
                put("edc.ids.id", "urn:connector:" + CONNECTOR_ID);
            }
        };
    }

    @BeforeEach
    void init() {
        Monitor monitor = EasyMock.createNiceMock(Monitor.class);
        EasyMock.replay(monitor);

        transformerRegistry = EasyMock.createNiceMock(TransformerRegistry.class);

        Vault vault = EasyMock.niceMock(Vault.class);
        var httpClient = new OkHttpClient.Builder().build();

        var idsWebhookAddress = "http://webhook";

        multipartDispatcher = new IdsMultipartRemoteMessageDispatcher();
        multipartDispatcher.register(new MultipartDescriptionRequestSender(CONNECTOR_ID, httpClient, OBJECT_MAPPER, monitor, identityService, transformerRegistry));
        multipartDispatcher.register(new MultipartArtifactRequestSender(CONNECTOR_ID, httpClient, OBJECT_MAPPER, monitor, vault, identityService, transformerRegistry));
        multipartDispatcher.register(new MultipartContractOfferSender(CONNECTOR_ID, httpClient, OBJECT_MAPPER, monitor, identityService, transformerRegistry, idsWebhookAddress));
        multipartDispatcher.register(new MultipartContractAgreementSender(CONNECTOR_ID, httpClient, OBJECT_MAPPER, monitor, identityService, transformerRegistry, idsWebhookAddress));
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

        var dataDestination = (DataAddress) EasyMock.createNiceMock(DataAddress.class);
        EasyMock.replay(dataDestination);

        EasyMock.expect(transformerRegistry.transform(EasyMock.anyObject(), EasyMock.anyObject()))
                .andReturn(new TransformResult<>(URI.create("urn:artifact:1")));
        EasyMock.expect(transformerRegistry.transform(EasyMock.anyObject(), EasyMock.anyObject()))
                .andReturn(new TransformResult<>(URI.create("urn:contract:1")));
        EasyMock.replay(transformerRegistry);

        var request = DataRequest.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(Protocols.IDS_MULTIPART)
                .contractId("1")
                .assetId(asset.getId())
                .dataDestination(dataDestination)
                .build();

        var result = multipartDispatcher.send(MultipartRequestInProcessResponse.class, request, () -> null).get();

        assertThat(result).isNotNull();
        assertThat(result.getHeader()).isNotNull();

        //TODO revise when handler for ArtifactRequestMessage exists
        assertThat(result.getHeader()).isInstanceOf(ResponseMessage.class);
        assertThat(result.getPayload()).isNull();
    }

    @Test
    void testSendContractOfferMessage() throws Exception {
        var contractOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        EasyMock.replay(contractOffer);

        EasyMock.expect(transformerRegistry.transform(EasyMock.anyObject(), EasyMock.anyObject()))
                .andReturn(new TransformResult<>(getIdsContractOffer()));
        EasyMock.replay(transformerRegistry);

        var request = ContractOfferRequest.Builder.newInstance()
                .type(ContractOfferRequest.Type.COUNTER_OFFER)
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(Protocols.IDS_MULTIPART)
                .contractOffer(contractOffer)
                .build();

        var result = multipartDispatcher.send(MultipartRequestInProcessResponse.class, request, () -> null).get();

        assertThat(result).isNotNull();
        assertThat(result.getHeader()).isNotNull();

        assertThat(result.getHeader()).isInstanceOf(RequestInProcessMessageImpl.class);
        assertThat(result.getPayload()).isNull();
    }

    @Test
    void testSendContractRequestMessage() throws Exception {
        var contractOffer = (ContractOffer) EasyMock.createNiceMock(ContractOffer.class);
        EasyMock.replay(contractOffer);

        EasyMock.expect(transformerRegistry.transform(EasyMock.anyObject(), EasyMock.anyObject()))
                .andReturn(new TransformResult<>(getIdsContractOffer()));
        EasyMock.replay(transformerRegistry);

        var request = ContractOfferRequest.Builder.newInstance()
                .type(ContractOfferRequest.Type.INITIAL)
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(Protocols.IDS_MULTIPART)
                .contractOffer(contractOffer)
                .build();

        var result = multipartDispatcher.send(MultipartRequestInProcessResponse.class, request, () -> null).get();

        assertThat(result).isNotNull();
        assertThat(result.getHeader()).isNotNull();

        assertThat(result.getHeader()).isInstanceOf(RequestInProcessMessageImpl.class);
        assertThat(result.getPayload()).isNull();
    }

    @Test
    void testSendContractAgreementMessage() throws Exception {
        var contractAgreement = (ContractAgreement) EasyMock.createNiceMock(ContractAgreement.class);
        EasyMock.replay(contractAgreement);

        EasyMock.expect(transformerRegistry.transform(EasyMock.anyObject(), EasyMock.anyObject()))
                .andReturn(new TransformResult<>(getIdsContractAgreement()));
        EasyMock.replay(transformerRegistry);

        var request = ContractAgreementRequest.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(Protocols.IDS_MULTIPART)
                .contractAgreement(contractAgreement)
                .correlationId("correlationId")
                .build();

        var result = multipartDispatcher.send(MultipartMessageProcessedResponse.class, request, () -> null).get();

        assertThat(result).isNotNull();
        assertThat(result.getHeader()).isNotNull();

        // TODO Fix as soon as validation works
        assertThat(result.getHeader()).isInstanceOf(RejectionMessageImpl.class);
        assertThat(result.getPayload()).isNull();
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

        // TODO Fix as soon as validation works
        assertThat(result.getHeader()).isInstanceOf(RejectionMessage.class);
        assertThat(((RejectionMessage) result.getHeader()).getRejectionReason())
                .isEqualByComparingTo(RejectionReason.BAD_PARAMETERS);
        assertThat(result.getPayload()).isNull();
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
        return new ContractAgreementBuilder()
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
