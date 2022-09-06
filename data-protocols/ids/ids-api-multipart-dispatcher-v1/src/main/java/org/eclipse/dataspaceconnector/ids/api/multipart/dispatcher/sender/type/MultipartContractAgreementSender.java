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

import de.fraunhofer.iais.eis.ContractAgreement;
import de.fraunhofer.iais.eis.ContractAgreementMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessageImpl;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.DelegateMessageContext;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.MultipartSenderDelegate;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.response.IdsMultipartParts;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.response.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil;
import org.eclipse.dataspaceconnector.ids.spi.domain.IdsConstants;
import org.eclipse.dataspaceconnector.ids.spi.types.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.types.IdsType;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreementRequest;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.util.ResponseUtil.parseMultipartStringResponse;
import static org.eclipse.dataspaceconnector.ids.spi.domain.IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY;

/**
 * IdsMultipartSender implementation for contract agreements. Sends IDS ContractAgreementMessages and
 * expects an IDS RequestInProcessMessage as the response.
 */
public class MultipartContractAgreementSender implements MultipartSenderDelegate<ContractAgreementRequest, String> {

    private final DelegateMessageContext context;

    public MultipartContractAgreementSender(@NotNull DelegateMessageContext context) {
        this.context = Objects.requireNonNull(context);
    }

    @Override
    public Class<ContractAgreementRequest> getMessageType() {
        return ContractAgreementRequest.class;
    }

    /**
     * Builds a {@link de.fraunhofer.iais.eis.ContractAgreementMessage} for the given {@link ContractAgreementRequest}.
     *
     * @param request the request.
     * @param token   the dynamic attribute token.
     * @return a ContractAgreementMessage.
     * @throws Exception if the agreement ID cannot be parsed.
     */
    @Override
    public Message buildMessageHeader(ContractAgreementRequest request, DynamicAttributeToken token) throws Exception {
        var idsId = IdsId.Builder.newInstance()
                .type(IdsType.CONTRACT_AGREEMENT)
                .value(request.getContractAgreement().getId())
                .build();

        var message = new ContractAgreementMessageBuilder(idsId.toUri())
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

    /**
     * Builds the payload for the agreement request. The payload contains the {@link ContractAgreement}.
     *
     * @param request the request.
     * @return the contract agreement as JSON-LD.
     * @throws Exception if parsing the agreement fails.
     */
    @Override
    public String buildMessagePayload(ContractAgreementRequest request) throws Exception {
        var transformationResult = context.getTransformerRegistry().transform(request, ContractAgreement.class);
        if (transformationResult.failed()) {
            throw new EdcException("Failed to create IDS contract agreement");
        }

        var idsContractAgreement = transformationResult.getContent();
        return context.getObjectMapper().writeValueAsString(idsContractAgreement);
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
        return List.of(MessageProcessedNotificationMessageImpl.class);
    }

}
