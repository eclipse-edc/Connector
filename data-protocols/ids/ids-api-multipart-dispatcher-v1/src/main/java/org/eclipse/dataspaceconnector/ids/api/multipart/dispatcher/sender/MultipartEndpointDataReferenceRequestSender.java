/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessageImpl;
import de.fraunhofer.iais.eis.ParticipantUpdateMessageBuilder;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.response.IdsMultipartParts;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.response.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.spi.domain.IdsConstants;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceMessage;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.util.ResponseUtil.parseMultipartStringResponse;

/**
 * IdsMultipartSender implementation for transferring Endpoint Data Reference (EDR). Sends IDS NotificationMessage and
 * expects an IDS MessageProcessedMessage as the response.
 */
public class MultipartEndpointDataReferenceRequestSender extends IdsMultipartSender<EndpointDataReferenceMessage, String> {
    private final TypeManager typeManager;

    public MultipartEndpointDataReferenceRequestSender(@NotNull String connectorId,
                                                       @NotNull OkHttpClient httpClient,
                                                       @NotNull ObjectMapper objectMapper,
                                                       @NotNull Monitor monitor,
                                                       @NotNull IdentityService identityService,
                                                       @NotNull IdsTransformerRegistry transformerRegistry,
                                                       @NotNull TypeManager typeManager) {
        super(connectorId, httpClient, objectMapper, monitor, identityService, transformerRegistry);

        this.typeManager = typeManager;
    }

    @Override
    public Class<EndpointDataReferenceMessage> messageType() {
        return EndpointDataReferenceMessage.class;
    }

    @Override
    protected String retrieveRemoteConnectorAddress(EndpointDataReferenceMessage request) {
        return request.getConnectorAddress();
    }

    /**
     * Builds a {@link de.fraunhofer.iais.eis.ParticipantUpdateMessage} for the given {@link EndpointDataReferenceMessage}.
     *
     * @param request the request.
     * @param token   the dynamic attribute token.
     * @return a ParticipantUpdateMessage.
     */
    @Override
    protected Message buildMessageHeader(EndpointDataReferenceMessage request, DynamicAttributeToken token) {
        return new ParticipantUpdateMessageBuilder()
                ._modelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._securityToken_(token)
                ._issuerConnector_(getConnectorId())
                ._senderAgent_(getConnectorId())
                ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                .build();
    }

    /**
     * Builds the payload for the endpoint data reference message. The payload contains the message as JSON.
     *
     * @param request the request.
     * @return the request as JSON.
     * @throws Exception if parsing the request fails.
     */
    @Override
    protected String buildMessagePayload(EndpointDataReferenceMessage request) throws Exception {
        // Note: EndpointDataReference is not an IDS object, so there is no need to serialize is with the IDS object mapper
        return typeManager.writeValueAsString(request.getEndpointDataReference());
    }

    /**
     * Parses the response content.
     *
     * @param parts container object for response header and payload InputStreams.
     * @return a MultipartResponse containing the message header and the response payload as string.
     * @throws Exception if parsing header or payload fails.
     */
    @Override
    protected MultipartResponse<String> getResponseContent(IdsMultipartParts parts) throws Exception {
        return parseMultipartStringResponse(parts, getObjectMapper());
    }

    @Override
    protected List<Class<? extends Message>> getAllowedResponseTypes() {
        return List.of(MessageProcessedNotificationMessageImpl.class);
    }
}
