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

package org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.type;

import de.fraunhofer.iais.eis.ContractAgreement;
import de.fraunhofer.iais.eis.ContractAgreementMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessageImpl;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.MultipartSenderDelegate;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.SenderDelegateContext;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.response.IdsMultipartParts;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.response.MultipartResponse;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.util.ResponseUtil;
import org.eclipse.edc.protocol.ids.spi.domain.IdsConstants;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.protocol.ids.util.CalendarUtil;
import org.eclipse.edc.spi.EdcException;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.eclipse.edc.protocol.ids.spi.domain.IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY;

/**
 * MultipartSenderDelegate for contract agreements.
 */
public class MultipartContractAgreementSender implements MultipartSenderDelegate<ContractAgreementMessage, String> {

    private final SenderDelegateContext context;

    public MultipartContractAgreementSender(SenderDelegateContext context) {
        this.context = context;
    }

    @Override
    public Class<ContractAgreementMessage> getMessageType() {
        return ContractAgreementMessage.class;
    }

    /**
     * Builds a {@link de.fraunhofer.iais.eis.ContractAgreementMessage} for the given {@link ContractAgreementMessage}.
     *
     * @param request the request.
     * @param token   the dynamic attribute token.
     * @return a ContractAgreementMessage.
     * @throws Exception if the agreement ID cannot be parsed.
     */
    @Override
    public Message buildMessageHeader(ContractAgreementMessage request, DynamicAttributeToken token) throws Exception {
        var idsId = IdsId.Builder.newInstance()
                .type(IdsType.CONTRACT_AGREEMENT)
                .value(request.getContractAgreement().getId())
                .build();

        var message = new ContractAgreementMessageBuilder(idsId.toUri())
                ._modelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._issued_(CalendarUtil.gregorianNow())
                ._securityToken_(token)
                ._issuerConnector_(context.getConnectorId().toUri())
                ._senderAgent_(context.getConnectorId().toUri())
                ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                ._transferContract_(URI.create(request.getProcessId()))
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
    public String buildMessagePayload(ContractAgreementMessage request) throws Exception {
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
        return ResponseUtil.parseMultipartStringResponse(parts, context.getObjectMapper());
    }

    @Override
    public List<Class<? extends Message>> getAllowedResponseTypes() {
        return List.of(MessageProcessedNotificationMessageImpl.class);
    }

}
