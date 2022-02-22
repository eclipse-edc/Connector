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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.ContractOfferMessageBuilder;
import de.fraunhofer.iais.eis.ContractRequestBuilder;
import de.fraunhofer.iais.eis.ContractRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message.MultipartRequestInProcessResponse;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.spi.IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY;

/**
 * IdsMultipartSender implementation for contract requests. Sends IDS ContractRequestMessages and
 * expects an IDS RequestInProcessMessage as the response.
 */
public class MultipartContractOfferSender extends IdsMultipartSender<ContractOfferRequest, MultipartRequestInProcessResponse> {

    private final String idsWebhookAddress;

    public MultipartContractOfferSender(@NotNull String connectorId,
                                        @NotNull OkHttpClient httpClient,
                                        @NotNull ObjectMapper objectMapper,
                                        @NotNull Monitor monitor,
                                        @NotNull IdentityService identityService,
                                        @NotNull TransformerRegistry transformerRegistry,
                                        @NotNull String idsWebhookAddress) {
        super(connectorId, httpClient, objectMapper, monitor, identityService, transformerRegistry);

        this.idsWebhookAddress = idsWebhookAddress;
    }

    @Override
    public Class<ContractOfferRequest> messageType() {
        return ContractOfferRequest.class;
    }

    @Override
    protected String retrieveRemoteConnectorId(ContractOfferRequest request) {
        return request.getConnectorId();
    }

    @Override
    protected String retrieveRemoteConnectorAddress(ContractOfferRequest request) {
        return request.getConnectorAddress();
    }

    @Override
    protected Message buildMessageHeader(ContractOfferRequest request, DynamicAttributeToken token) {
        if (request.getType() == ContractOfferRequest.Type.INITIAL) {
            var message = new ContractRequestMessageBuilder()
                    ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                    //._issued_(gregorianNow()) TODO once https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/236 is done
                    ._securityToken_(token)
                    ._issuerConnector_(getConnectorId())
                    ._senderAgent_(getConnectorId())
                    ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                    ._transferContract_(URI.create(request.getCorrelationId()))
                    .build();
            message.setProperty(IDS_WEBHOOK_ADDRESS_PROPERTY, idsWebhookAddress + "/api/ids/multipart");

            return message;
        } else {
            var message = new ContractOfferMessageBuilder()
                    ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                    //._issued_(gregorianNow()) TODO once https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/236 is done
                    ._securityToken_(token)
                    ._issuerConnector_(getConnectorId())
                    ._senderAgent_(getConnectorId())
                    ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                    ._transferContract_(URI.create(request.getCorrelationId()))
                    .build();
            message.setProperty(IDS_WEBHOOK_ADDRESS_PROPERTY, idsWebhookAddress + "/api/ids/multipart");

            return message;
        }
    }

    @Override
    protected String buildMessagePayload(ContractOfferRequest request) throws Exception {
        var contractOffer = request.getContractOffer();

        if (request.getType() == ContractOfferRequest.Type.INITIAL) {
            return getObjectMapper().writeValueAsString(createContractRequest(contractOffer));
        } else {
            return getObjectMapper().writeValueAsString(createContractOffer(contractOffer));
        }
    }

    @Override
    protected MultipartRequestInProcessResponse getResponseContent(IdsMultipartParts parts) throws Exception {
        Message header = getObjectMapper().readValue(parts.getHeader(), Message.class);
        String payload = null;
        if (parts.getPayload() != null) {
            payload = new String(parts.getPayload().readAllBytes());
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
