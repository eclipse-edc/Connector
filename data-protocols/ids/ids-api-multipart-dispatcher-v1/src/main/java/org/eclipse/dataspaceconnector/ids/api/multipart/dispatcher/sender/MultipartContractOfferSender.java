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

package org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender;

import de.fraunhofer.iais.eis.ContractOfferMessageBuilder;
import de.fraunhofer.iais.eis.ContractRequestBuilder;
import de.fraunhofer.iais.eis.ContractRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message.MultipartRequestInProcessResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferMessage;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil.gregorianNow;

/**
 * IdsMultipartSender implementation for contract requests. Sends IDS ContractRequestMessages and
 * expects an IDS RequestInProcessMessage as the response.
 */
public class MultipartContractOfferSender extends IdsMultipartSender<ContractOfferMessage, MultipartRequestInProcessResponse> {

    public MultipartContractOfferSender(@NotNull String connectorId,
                                        @NotNull OkHttpClient httpClient,
                                        @NotNull Serializer serializer,
                                        @NotNull Monitor monitor,
                                        @NotNull IdentityService identityService,
                                        @NotNull TransformerRegistry transformerRegistry,
                                        @NotNull String idsWebhookAddress) {
        super(connectorId, idsWebhookAddress, httpClient, monitor, identityService, transformerRegistry, serializer);
    }

    @Override
    public Class<ContractOfferMessage> messageType() {
        return ContractOfferMessage.class;
    }

    @Override
    protected String retrieveRemoteConnectorId(ContractOfferMessage request) {
        return request.getConnectorId();
    }

    @Override
    protected String retrieveRemoteConnectorAddress(ContractOfferMessage request) {
        return request.getConnectorAddress();
    }

    @Override
    protected Message buildMessageHeader(ContractOfferMessage request, DynamicAttributeToken token) {

        String messageId = request.getContractOffer().getProperty(ContractOffer.PROPERTY_MESSAGE_ID);
        if (messageId == null) {
            throw new EdcException("Cannot send out an ContractOffer without trackable message id");
        }

        IdsId msgId = IdsId.Builder.newInstance().type(IdsType.MESSAGE).value(messageId).build();
        Result<URI> msgUri = getTransformerRegistry().transform(msgId, URI.class);
        if (msgUri.failed()) {
            throw new EdcException("Cannot convert message id to URI");
        }

        if (request.getType() == ContractOfferMessage.Type.INITIAL) {

            ContractRequestMessageBuilder builder = new ContractRequestMessageBuilder(msgUri.getContent())
                    ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                    ._issued_(gregorianNow())
                    ._securityToken_(token)
                    ._issuerConnector_(getConnectorId())
                    ._senderAgent_(getSenderAgentURI())
                    ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())));

            return builder.build();
        } else {

            ContractOfferMessageBuilder builder = new ContractOfferMessageBuilder(msgUri.getContent())
                    ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                    ._issued_(gregorianNow())
                    ._securityToken_(token)
                    ._issuerConnector_(getConnectorId())
                    ._senderAgent_(getSenderAgentURI())
                    ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())));

            return builder.build();
        }
    }


    @Override
    protected String buildMessagePayload(ContractOfferMessage request) throws Exception {
        var contractOffer = request.getContractOffer();

        if (request.getType() == ContractOfferMessage.Type.INITIAL) {
            return getSerializer().serialize(createContractRequest(contractOffer));
        } else {
            return getSerializer().serialize(createContractOffer(contractOffer));
        }
    }

    @Override
    protected MultipartRequestInProcessResponse getResponseContent(IdsMultipartParts parts) throws Exception {
        Message header = getSerializer().deserialize(parts.getHeader(), Message.class);
        String payload = null;
        if (parts.getPayload() != null) {
            payload = parts.getPayload();
        }

        return MultipartRequestInProcessResponse.Builder.newInstance()
                .header(header)
                .payload(payload)
                .build();
    }

    private de.fraunhofer.iais.eis.ContractRequest createContractRequest(ContractOffer offer) {
        var transformationResult = getTransformerRegistry().transform(offer, de.fraunhofer.iais.eis.ContractOffer.class);
        if (transformationResult.failed()) {
            throw new EdcException("Failed to create IDS contract request");
        }

        return createIdsRequestFromOffer(Objects.requireNonNull(transformationResult.getContent(),
                "Transformer output is null"));
    }

    private de.fraunhofer.iais.eis.ContractOffer createContractOffer(ContractOffer offer) {
        var transformationResult = getTransformerRegistry().transform(offer, de.fraunhofer.iais.eis.ContractOffer.class);
        if (transformationResult.failed()) {
            throw new EdcException("Failed to create IDS contract offer");
        }

        return transformationResult.getContent();
    }

    private de.fraunhofer.iais.eis.ContractRequest createIdsRequestFromOffer(de.fraunhofer.iais.eis.ContractOffer offer) {
        return new ContractRequestBuilder(offer.getId())
                ._consumer_(offer.getConsumer())
                ._contractAnnex_(offer.getContractAnnex())
                ._contractDate_(offer.getContractDate())
                ._contractEnd_(offer.getContractEnd())
                ._contractDocument_(offer.getContractDocument())
                ._contractStart_(offer.getContractStart())
                ._obligation_(offer.getObligation())
                ._permission_(offer.getPermission())
                ._prohibition_(offer.getProhibition())
                ._provider_(offer.getProvider())
                .build();
    }
}
