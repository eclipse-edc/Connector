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

package org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.type;

import de.fraunhofer.iais.eis.ArtifactRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RequestInProcessMessageImpl;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.SenderDelegateContext;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.MultipartSenderDelegate;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.response.IdsMultipartParts;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.response.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil;
import org.eclipse.dataspaceconnector.ids.spi.domain.IdsConstants;
import org.eclipse.dataspaceconnector.ids.spi.types.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.types.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.types.container.ArtifactRequestMessagePayload;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.util.ResponseUtil.parseMultipartStringResponse;
import static org.eclipse.dataspaceconnector.ids.spi.domain.IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY;

/**
 * MultipartSenderDelegate for data requests.
 */
public class MultipartArtifactRequestSender implements MultipartSenderDelegate<DataRequest, String> {

    private final SenderDelegateContext context;
    private final Vault vault;

    public MultipartArtifactRequestSender(@NotNull SenderDelegateContext context, @NotNull Vault vault) {
        this.context = Objects.requireNonNull(context);
        this.vault = Objects.requireNonNull(vault);
    }

    @Override
    public Class<DataRequest> getMessageType() {
        return DataRequest.class;
    }

    /**
     * Builds an {@link de.fraunhofer.iais.eis.ArtifactRequestMessage} for the given {@link DataRequest}.
     *
     * @param request the request.
     * @param token   the dynamic attribute token.
     * @return an ArtifactRequestMessage
     */
    @Override
    public Message buildMessageHeader(DataRequest request, DynamicAttributeToken token) {
        var artifactId = IdsId.Builder.newInstance()
                .value(request.getAssetId())
                .type(IdsType.ARTIFACT)
                .build().toUri();
        var contractId = IdsId.Builder.newInstance()
                .value(request.getContractId())
                .type(IdsType.CONTRACT_AGREEMENT)
                .build().toUri();
 
        var artifactRequestId = request.getId() != null ? request.getId() : UUID.randomUUID().toString();
        var message = new ArtifactRequestMessageBuilder(URI.create(artifactRequestId))
                ._modelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._issued_(CalendarUtil.gregorianNow())
                ._securityToken_(token)
                ._issuerConnector_(context.getConnectorId())
                ._senderAgent_(context.getConnectorId())
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
    public String buildMessagePayload(DataRequest request) throws Exception {
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
        return parseMultipartStringResponse(parts, context.getObjectMapper());
    }

    @Override
    public List<Class<? extends Message>> getAllowedResponseTypes() {
        return List.of(RequestInProcessMessageImpl.class);
    }
}
