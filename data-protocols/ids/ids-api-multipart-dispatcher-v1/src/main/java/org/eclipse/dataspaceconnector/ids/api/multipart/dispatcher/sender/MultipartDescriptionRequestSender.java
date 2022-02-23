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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.BaseConnector;
import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ModelClass;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceCatalog;
import de.fraunhofer.iais.eis.ResponseMessage;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message.MultipartDescriptionResponse;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.MetadataRequest;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil.gregorianNow;

/**
 * IdsMultipartSender implementation for metadata requests. Sends IDS DescriptionRequestMessages and
 * expects an IDS DescriptionResponseMessage as the response.
 */
public class MultipartDescriptionRequestSender extends IdsMultipartSender<MetadataRequest, MultipartDescriptionResponse> {

    private final ObjectMapper objectMapper;

    public MultipartDescriptionRequestSender(@NotNull String connectorId,
                                             @NotNull String idsWebhookAddress,
                                             @NotNull OkHttpClient httpClient,
                                             @NotNull Serializer serializer,
                                             @NotNull Monitor monitor,
                                             @NotNull IdentityService identityService,
                                             @NotNull TransformerRegistry transformerRegistry, ObjectMapper objectMapper) {
        super(connectorId, idsWebhookAddress, httpClient, monitor, identityService, transformerRegistry, serializer);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public Class<MetadataRequest> messageType() {
        return MetadataRequest.class;
    }

    @Override
    protected String retrieveRemoteConnectorId(MetadataRequest request) {
        return request.getConnectorId();
    }

    @Override
    protected String retrieveRemoteConnectorAddress(MetadataRequest request) {
        return request.getConnectorAddress();
    }

    @Override
    protected Message buildMessageHeader(MetadataRequest request, DynamicAttributeToken token) {
        return new DescriptionRequestMessageBuilder()
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._issued_(gregorianNow())
                ._securityToken_(token)
                ._issuerConnector_(getConnectorId())
                ._senderAgent_(getSenderAgentURI())
                ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                ._requestedElement_(request.getRequestedAsset())
                .build();
    }

    @Override
    protected MultipartDescriptionResponse getResponseContent(IdsMultipartParts parts) throws Exception {
        ResponseMessage header = getSerializer().deserialize(parts.getHeader(), ResponseMessage.class);

        ModelClass payload = null;
        if (parts.getPayload() != null) {
            String payloadString = parts.getPayload();
            JsonNode payloadJson = objectMapper.readTree(payloadString);
            JsonNode type = payloadJson.get("@type");
            switch (type.textValue()) {
                case "ids:BaseConnector":
                    payload = getSerializer().deserialize(payloadString, BaseConnector.class);
                    break;
                case "ids:ResourceCatalog":
                    payload = getSerializer().deserialize(payloadString, ResourceCatalog.class);
                    break;
                case "ids:Resource":
                    payload = getSerializer().deserialize(payloadString, Resource.class);
                    break;
                case "ids:Representation":
                    payload = getSerializer().deserialize(payloadString, Representation.class);
                    break;
                case "ids:Artifact":
                    payload = getSerializer().deserialize(payloadString, Artifact.class);
                    break;
                default:
                    throw new EdcException(String.format("Unknown type: %s", type.textValue()));
            }
        }

        return MultipartDescriptionResponse.Builder.newInstance()
                .header(header)
                .payload(payload)
                .build();
    }
}
