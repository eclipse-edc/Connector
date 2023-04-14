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

import de.fraunhofer.iais.eis.ContractRejectionMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessageImpl;
import de.fraunhofer.iais.eis.util.TypedLiteral;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.MultipartSenderDelegate;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.SenderDelegateContext;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.response.IdsMultipartParts;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.response.MultipartResponse;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.util.ResponseUtil;
import org.eclipse.edc.protocol.ids.spi.domain.IdsConstants;
import org.eclipse.edc.protocol.ids.util.CalendarUtil;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * MultipartSenderDelegate for contract rejections.
 */
public class MultipartContractRejectionSender implements MultipartSenderDelegate<ContractNegotiationTerminationMessage, String> {

    private final SenderDelegateContext context;

    public MultipartContractRejectionSender(SenderDelegateContext context) {
        this.context = context;
    }

    @Override
    public Class<ContractNegotiationTerminationMessage> getMessageType() {
        return ContractNegotiationTerminationMessage.class;
    }

    /**
     * Builds a {@link de.fraunhofer.iais.eis.ContractRejectionMessage} for the given {@link ContractNegotiationTerminationMessage}.
     *
     * @param rejection the rejection request.
     * @param token   the dynamic attribute token.
     * @return a ContractRejectionMessage
     */
    @Override
    public Message buildMessageHeader(ContractNegotiationTerminationMessage rejection, DynamicAttributeToken token) throws Exception {
        return new ContractRejectionMessageBuilder()
                ._modelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._issued_(CalendarUtil.gregorianNow())
                ._securityToken_(token)
                ._issuerConnector_(context.getConnectorId().toUri())
                ._senderAgent_(context.getConnectorId().toUri())
                ._recipientConnector_(Collections.singletonList(URI.create(rejection.getConnectorId())))
                ._contractRejectionReason_(new TypedLiteral(rejection.getRejectionReason()))
                ._transferContract_(URI.create(rejection.getCorrelationId()))
                .build();
    }

    /**
     * Builds the payload for the rejection. The payload contains the rejection reason.
     *
     * @param rejection the rejection request.
     * @return the rejection reason.
     */
    @Override
    public String buildMessagePayload(ContractNegotiationTerminationMessage rejection) throws Exception {
        return rejection.getRejectionReason();
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
