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

package org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.type;

import de.fraunhofer.iais.eis.ContractOfferMessageBuilder;
import de.fraunhofer.iais.eis.ContractRequest;
import de.fraunhofer.iais.eis.ContractRequestBuilder;
import de.fraunhofer.iais.eis.ContractRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RequestInProcessMessageImpl;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.DelegateMessageContext;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.MultipartSenderDelegate;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.response.IdsMultipartParts;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.response.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil;
import org.eclipse.dataspaceconnector.ids.spi.domain.IdsConstants;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.util.ResponseUtil.parseMultipartStringResponse;
import static org.eclipse.dataspaceconnector.ids.spi.domain.IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY;

/**
 * MultipartSenderDelegate for contract requests.
 */
public class MultipartContractOfferSender implements MultipartSenderDelegate<ContractOfferRequest, String> {
    private final DelegateMessageContext context;

    public MultipartContractOfferSender(@NotNull DelegateMessageContext context) {
        this.context = Objects.requireNonNull(context);
    }

    @Override
    public Class<ContractOfferRequest> getMessageType() {
        return ContractOfferRequest.class;
    }

    /**
     * Builds a {@link de.fraunhofer.iais.eis.ContractRequestMessage} or a {@link de.fraunhofer.iais.eis.ContractOfferMessage}
     * for the given {@link ContractOfferRequest} depending on whether it is an initial request.
     *
     * @param request the request.
     * @param token   the dynamic attribute token.
     * @return a ContractRequestMessage or ContractOfferMessage
     */
    @Override
    public Message buildMessageHeader(ContractOfferRequest request, DynamicAttributeToken token) {
        if (request.getType() == ContractOfferRequest.Type.INITIAL) {
            var message = new ContractRequestMessageBuilder()
                    ._modelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                    ._issued_(CalendarUtil.gregorianNow())
                    ._securityToken_(token)
                    ._issuerConnector_(context.getConnectorId())
                    ._senderAgent_(context.getConnectorId())
                    ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                    ._transferContract_(URI.create(request.getCorrelationId()))
                    .build();
            message.setProperty(IDS_WEBHOOK_ADDRESS_PROPERTY, context.getIdsWebhookAddress());
  
            return message;
        } else {
            var message = new ContractOfferMessageBuilder()
                    ._modelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                    ._issued_(CalendarUtil.gregorianNow())
                    ._securityToken_(token)
                    ._issuerConnector_(context.getConnectorId())
                    ._senderAgent_(context.getConnectorId())
                    ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                    ._transferContract_(URI.create(request.getCorrelationId()))
                    .build();
            message.setProperty(IDS_WEBHOOK_ADDRESS_PROPERTY, context.getIdsWebhookAddress());

            return message;
        }
    }

    /**
     * Builds the payload for the contract offer request. The payload contains either a {@link de.fraunhofer.iais.eis.ContractRequest}
     * or a {@link de.fraunhofer.iais.eis.ContractOffer} depending on whether it is an initial request.
     *
     * @param request the request.
     * @return the contract request/offer as JSON-LD.
     * @throws Exception if parsing the request/offer fails.
     */
    @Override
    public String buildMessagePayload(ContractOfferRequest request) throws Exception {
        var contractOffer = request.getContractOffer();
        
        if (request.getType() == ContractOfferRequest.Type.INITIAL) {
            return context.getObjectMapper().writeValueAsString(createContractRequest(contractOffer));
        } else {
            return context.getObjectMapper().writeValueAsString(createContractOffer(contractOffer));
        }
    }

    /**
     * Parses the response content.
     *
     * @param parts container object for response header and payload InputStreams.
     * @return a MultipartResponse containing the message header and the response payload as string.
     * @throws Exception if parsing header or payload fails.
     */
    @Override
    public MultipartResponse<String> getResponseContent(IdsMultipartParts parts) throws Exception {
        return parseMultipartStringResponse(parts, context.getObjectMapper());
    }

    @Override
    public List<Class<? extends Message>> getAllowedResponseTypes() {
        return List.of(RequestInProcessMessageImpl.class);
    }

    private ContractRequest createContractRequest(ContractOffer offer) {
        var transformationResult = context.getTransformerRegistry().transform(offer, de.fraunhofer.iais.eis.ContractOffer.class);
        if (transformationResult.failed()) {
            throw new EdcException("Failed to create IDS contract request");
        }

        return createIdsRequestFromOffer(Objects.requireNonNull(transformationResult.getContent(),
                "Transformer output is null"));
    }

    private de.fraunhofer.iais.eis.ContractOffer createContractOffer(ContractOffer offer) {
        var transformationResult = context.getTransformerRegistry().transform(offer, de.fraunhofer.iais.eis.ContractOffer.class);
        if (transformationResult.failed()) {
            throw new EdcException("Failed to create IDS contract offer");
        }

        return transformationResult.getContent();
    }

    private ContractRequest createIdsRequestFromOffer(de.fraunhofer.iais.eis.ContractOffer offer) {
        var request = new ContractRequestBuilder(offer.getId())
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
    
        if (offer.getProperties() != null) {
            offer.getProperties().forEach(request::setProperty);
        }
    
        return request;
    }
}
