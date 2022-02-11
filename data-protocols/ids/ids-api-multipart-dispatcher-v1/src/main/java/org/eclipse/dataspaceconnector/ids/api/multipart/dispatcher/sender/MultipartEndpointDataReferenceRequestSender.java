/*
 *  Copyright (c) 2020, 2021 Fraunhofer Institute for Software and Systems Engineering
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
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ParticipantUpdateMessageBuilder;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message.MultipartMessageProcessedResponse;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceRequest;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;

/**
 * IdsMultipartSender implementation for transferring Endpoint Data Reference (EDR). Sends IDS NotificationMessage and
 * expects an IDS DescriptionResponseMessage as the response.
 */
public class MultipartEndpointDataReferenceRequestSender extends IdsMultipartSender<EndpointDataReferenceRequest, MultipartMessageProcessedResponse> {

    public MultipartEndpointDataReferenceRequestSender(@NotNull String connectorId,
                                                       @NotNull OkHttpClient httpClient,
                                                       @NotNull ObjectMapper objectMapper,
                                                       @NotNull Monitor monitor,
                                                       @NotNull IdentityService identityService,
                                                       @NotNull TransformerRegistry transformerRegistry) {
        super(connectorId, httpClient, objectMapper, monitor, identityService, transformerRegistry);
    }

    @Override
    public Class<EndpointDataReferenceRequest> messageType() {
        return EndpointDataReferenceRequest.class;
    }

    @Override
    protected String retrieveRemoteConnectorId(EndpointDataReferenceRequest request) {
        return request.getConnectorId();
    }

    @Override
    protected String retrieveRemoteConnectorAddress(EndpointDataReferenceRequest request) {
        return request.getConnectorAddress();
    }

    @Override
    protected Message buildMessageHeader(EndpointDataReferenceRequest request, DynamicAttributeToken token) {
        return new ParticipantUpdateMessageBuilder()
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._securityToken_(token)
                ._issuerConnector_(getConnectorId())
                ._senderAgent_(getConnectorId())
                ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                .build();
    }

    @Override
    protected String buildMessagePayload(EndpointDataReferenceRequest request) throws Exception {
        return getObjectMapper().writeValueAsString(request.getEndpointDataReference());
    }

    @Override
    protected MultipartMessageProcessedResponse getResponseContent(IdsMultipartParts parts) throws Exception {
        var header = getObjectMapper().readValue(parts.getHeader(), Message.class);
        String payload = null;
        if (parts.getPayload() != null) {
            payload = new String(parts.getPayload().readAllBytes());
        }

        return MultipartMessageProcessedResponse.Builder.newInstance()
                .header(header)
                .payload(payload)
                .build();
    }
}
