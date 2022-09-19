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

import de.fraunhofer.iais.eis.ContractRejectionMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessageImpl;
import de.fraunhofer.iais.eis.util.TypedLiteral;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.MultipartSenderDelegate;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.SenderDelegateContext;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.response.IdsMultipartParts;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.response.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil;
import org.eclipse.dataspaceconnector.ids.spi.domain.IdsConstants;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractRejection;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.util.ResponseUtil.parseMultipartStringResponse;

/**
 * MultipartSenderDelegate for contract rejections.
 */
public class MultipartContractRejectionSender implements MultipartSenderDelegate<ContractRejection, String> {

    private final SenderDelegateContext context;

    public MultipartContractRejectionSender(SenderDelegateContext context) {
        this.context = context;
    }

    @Override
    public Class<ContractRejection> getMessageType() {
        return ContractRejection.class;
    }

    /**
     * Builds a {@link de.fraunhofer.iais.eis.ContractRejectionMessage} for the given {@link ContractRejection}.
     *
     * @param rejection the rejection request.
     * @param token   the dynamic attribute token.
     * @return a ContractRejectionMessage
     */
    @Override
    public Message buildMessageHeader(ContractRejection rejection, DynamicAttributeToken token) throws Exception {
        return new ContractRejectionMessageBuilder()
                ._modelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._issued_(CalendarUtil.gregorianNow())
                ._securityToken_(token)
                ._issuerConnector_(context.getConnectorId())
                ._senderAgent_(context.getConnectorId())
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
    public String buildMessagePayload(ContractRejection rejection) throws Exception {
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
        return parseMultipartStringResponse(parts, context.getObjectMapper());
    }

    @Override
    public List<Class<? extends Message>> getAllowedResponseTypes() {
        return List.of(MessageProcessedNotificationMessageImpl.class);
    }
}
