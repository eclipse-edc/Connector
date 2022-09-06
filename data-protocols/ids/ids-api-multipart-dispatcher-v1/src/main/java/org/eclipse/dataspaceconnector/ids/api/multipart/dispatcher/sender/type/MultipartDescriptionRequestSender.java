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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.BaseConnector;
import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import de.fraunhofer.iais.eis.DescriptionResponseMessageImpl;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ModelClass;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceCatalog;
import de.fraunhofer.iais.eis.ResponseMessage;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.DelegateMessageContext;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.MultipartSenderDelegate;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.response.IdsMultipartParts;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.response.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil;
import org.eclipse.dataspaceconnector.ids.spi.domain.IdsConstants;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.MetadataRequest;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * IdsMultipartSender implementation for metadata requests. Sends IDS DescriptionRequestMessages and
 * expects an IDS DescriptionResponseMessage as the response.
 */
public class MultipartDescriptionRequestSender implements MultipartSenderDelegate<MetadataRequest, ModelClass> {

    private final DelegateMessageContext context;

    public MultipartDescriptionRequestSender(@NotNull DelegateMessageContext context) {
        this.context = Objects.requireNonNull(context);
    }

    @Override
    public Class<MetadataRequest> getMessageType() {
        return MetadataRequest.class;
    }

    /**
     * Builds a {@link de.fraunhofer.iais.eis.DescriptionRequestMessage} for the given {@link MetadataRequest}.
     *
     * @param request the request.
     * @param token   the dynamic attribute token.
     * @return a DescriptionRequestMessage.
     */
    @Override
    public Message buildMessageHeader(MetadataRequest request, DynamicAttributeToken token) {
        return new DescriptionRequestMessageBuilder()
                ._modelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._issued_(CalendarUtil.gregorianNow())
                ._securityToken_(token)
                ._issuerConnector_(context.getConnectorId())
                ._senderAgent_(context.getConnectorId())
                ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                ._requestedElement_(request.getRequestedAsset())
                .build();
    }

    /**
     * Parses the response content. Tries to parse the payload to the correct IDS information model type.
     *
     * @param parts container object for response header and payload InputStreams.
     * @return a MultipartResponse containing the message header and the information model object representing the payload.
     * @throws Exception if parsing header or payload fails or the payload type cannot be determined.
     */
    @Override
    public MultipartResponse<ModelClass> getResponseContent(IdsMultipartParts parts) throws Exception {
        ObjectMapper objectMapper = context.getObjectMapper();

        ResponseMessage header = objectMapper.readValue(parts.getHeader(), ResponseMessage.class);

        ModelClass payload = null;
        if (parts.getPayload() != null) {
            String payloadString = new String(parts.getPayload().readAllBytes());
            JsonNode payloadJson = objectMapper.readTree(payloadString);
            JsonNode type = payloadJson.get("@type");
            switch (type.textValue()) {
                case "ids:BaseConnector":
                    payload = objectMapper.readValue(payloadString, BaseConnector.class);
                    break;
                case "ids:ResourceCatalog":
                    payload = objectMapper.readValue(payloadString, ResourceCatalog.class);
                    break;
                case "ids:Resource":
                    payload = objectMapper.readValue(payloadString, Resource.class);
                    break;
                case "ids:Representation":
                    payload = objectMapper.readValue(payloadString, Representation.class);
                    break;
                case "ids:Artifact":
                    payload = objectMapper.readValue(payloadString, Artifact.class);
                    break;
                default:
                    throw new EdcException(String.format("Unknown type: %s", type.textValue()));
            }
        }

        return new MultipartResponse<>(header, payload);
    }

    @Override
    public List<Class<? extends Message>> getAllowedResponseTypes() {
        return List.of(DescriptionResponseMessageImpl.class);
    }
}
