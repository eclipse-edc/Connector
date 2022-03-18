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
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message.MultipartDescriptionResponse;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.MetadataRequest;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;

/**
 * IdsMultipartSender implementation for metadata requests. Sends IDS DescriptionRequestMessages and
 * expects an IDS DescriptionResponseMessage as the response.
 */
public class MultipartDescriptionRequestSender extends IdsMultipartSender<MetadataRequest, MultipartDescriptionResponse> {

    public MultipartDescriptionRequestSender(@NotNull String connectorId,
                                             @NotNull OkHttpClient httpClient,
                                             @NotNull ObjectMapper objectMapper,
                                             @NotNull Monitor monitor,
                                             @NotNull IdentityService identityService,
                                             @NotNull IdsTransformerRegistry transformerRegistry) {
        super(connectorId, httpClient, objectMapper, monitor, identityService, transformerRegistry);
    }

    @Override
    public Class<MetadataRequest> messageType() {
        return MetadataRequest.class;
    }

    @Override
    protected String retrieveRemoteConnectorAddress(MetadataRequest request) {
        return request.getConnectorAddress();
    }

    @Override
    protected Message buildMessageHeader(MetadataRequest request, DynamicAttributeToken token) {
        return new DescriptionRequestMessageBuilder()
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                //._issued_(gregorianNow()) TODO once https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/236 is done
                ._securityToken_(token)
                ._issuerConnector_(getConnectorId())
                ._senderAgent_(getConnectorId())
                ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                ._requestedElement_(request.getRequestedAsset())
                .build();
    }

    @Override
    protected MultipartDescriptionResponse getResponseContent(IdsMultipartParts parts) throws Exception {
        ObjectMapper objectMapper = getObjectMapper();

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

        return MultipartDescriptionResponse.Builder.newInstance()
                .header(header)
                .payload(payload)
                .build();
    }
}
