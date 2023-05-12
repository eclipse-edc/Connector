/*
 *  Copyright (c) 2020 - 2022 Fraunhofer Institute for Software and Systems Engineering
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

import de.fraunhofer.iais.eis.ArtifactRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RequestInProcessMessageImpl;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.MultipartSenderDelegate;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.SenderDelegateContext;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.response.IdsMultipartParts;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.response.MultipartResponse;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.util.ResponseUtil;
import org.eclipse.edc.protocol.ids.spi.domain.IdsConstants;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.protocol.ids.spi.types.container.ArtifactRequestMessagePayload;
import org.eclipse.edc.protocol.ids.util.CalendarUtil;
import org.eclipse.edc.spi.security.Vault;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.eclipse.edc.protocol.ids.spi.domain.IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY;

/**
 * MultipartSenderDelegate for data requests.
 */
public class MultipartTransferRequestSender implements MultipartSenderDelegate<TransferRequestMessage, String> {

    private final SenderDelegateContext context;
    private final Vault vault;

    public MultipartTransferRequestSender(SenderDelegateContext context, Vault vault) {
        this.context = context;
        this.vault = vault;
    }

    @Override
    public Class<TransferRequestMessage> getMessageType() {
        return TransferRequestMessage.class;
    }

    /**
     * Builds an {@link de.fraunhofer.iais.eis.ArtifactRequestMessage} for the given {@link TransferRequestMessage}.
     *
     * @param request the request.
     * @param token   the dynamic attribute token.
     * @return an ArtifactRequestMessage
     */
    @Override
    public Message buildMessageHeader(TransferRequestMessage request, DynamicAttributeToken token) {
        var artifactId = IdsId.Builder.newInstance()
                .value(request.getAssetId())
                .type(IdsType.ARTIFACT)
                .build().toUri();
        var contractId = IdsId.Builder.newInstance()
                .value(request.getContractId())
                .type(IdsType.CONTRACT_AGREEMENT)
                .build().toUri();

        var artifactRequestId = request.getProcessId() != null ? request.getProcessId() : UUID.randomUUID().toString();
        var message = new ArtifactRequestMessageBuilder(URI.create(artifactRequestId))
                ._modelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._issued_(CalendarUtil.gregorianNow())
                ._securityToken_(token)
                ._issuerConnector_(context.getConnectorId().toUri())
                ._senderAgent_(context.getConnectorId().toUri())
                ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                ._requestedArtifact_(artifactId)
                ._transferContract_(contractId)
                .build();

        message.setProperty(IDS_WEBHOOK_ADDRESS_PROPERTY, context.getIdsWebhookAddress());

        request.getProperties().forEach(message::setProperty);
        return message;
    }

    /**
     * Builds the payload for the artifact request. The payload contains the data destination and a secret key.
     *
     * @param request the request.
     * @return the message payload.
     * @throws Exception if parsing the payload fails.
     */
    @Override
    public String buildMessagePayload(TransferRequestMessage request) throws Exception {
        var requestPayloadBuilder = ArtifactRequestMessagePayload.Builder.newInstance()
                .dataDestination(request.getDataDestination());

        if (request.getDataDestination().getKeyName() != null) {
            String secret = vault.resolveSecret(request.getDataDestination().getKeyName());
            requestPayloadBuilder = requestPayloadBuilder.secret(secret);
        }

        return context.getObjectMapper().writeValueAsString(requestPayloadBuilder.build());
    }

    /**
     * Parses the response content.
     *
     * @param parts container object for response header and payload input streams.
     * @return a MultipartResponse containing the message header and the response payload as string.
     * @throws Exception if parsing header or payload fails.
     */
    @Override
    public MultipartResponse<String> getResponseContent(IdsMultipartParts parts) throws Exception {
        return ResponseUtil.parseMultipartStringResponse(parts, context.getObjectMapper());
    }

    @Override
    public List<Class<? extends Message>> getAllowedResponseTypes() {
        return List.of(RequestInProcessMessageImpl.class);
    }
}
