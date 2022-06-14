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
import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.BaseConnector;
import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceCatalog;
import de.fraunhofer.iais.eis.ResponseMessage;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message.MultipartDescriptionResponse;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.serializer.jsonld.JsonldSerializer;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.MetadataRequest;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * IdsMultipartSender implementation for metadata requests. Sends IDS DescriptionRequestMessages and
 * expects an IDS DescriptionResponseMessage as the response.
 */
public class MultipartDescriptionRequestSender extends IdsMultipartSender<MetadataRequest, MultipartDescriptionResponse> {
    private final JsonldSerializer serializer;

    public MultipartDescriptionRequestSender(@NotNull String connectorId,
                                             @NotNull OkHttpClient httpClient,
                                             @NotNull JsonldSerializer serializer,
                                             @NotNull Monitor monitor,
                                             @NotNull IdentityService identityService,
                                             @NotNull IdsTransformerRegistry transformerRegistry) {
        super(connectorId, httpClient, monitor, identityService, transformerRegistry);

        this.serializer = serializer;
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
        var header = (ResponseMessage) serializer.deserialize(new String(parts.getHeader().readAllBytes(), StandardCharsets.UTF_8), ResponseMessage.class);

        var builder = MultipartDescriptionResponse.Builder.newInstance();
        builder.header(header);

        if (parts.getPayload() != null) {
            String payloadString = new String(parts.getPayload().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode payloadJson = serializer.getObjectMapper().readTree(payloadString);
            JsonNode type = payloadJson.get("@type");
            switch (type.textValue()) {
                case "ids:BaseConnector":
                    builder.payload((BaseConnector) serializer.deserialize(payloadString, BaseConnector.class));
                    break;
                case "ids:ResourceCatalog":
                    builder.payload((ResourceCatalog) serializer.deserialize(payloadString, ResourceCatalog.class));
                    break;
                case "ids:Resource":
                    builder.payload((Resource) serializer.deserialize(payloadString, Resource.class));
                    break;
                case "ids:Representation":
                    builder.payload((Representation) serializer.deserialize(payloadString, Representation.class));
                    break;
                case "ids:Artifact":
                    builder.payload((Artifact) serializer.deserialize(payloadString, Artifact.class));
                    break;
                default:
                    throw new EdcException(String.format("Unknown type: %s", type.textValue()));
            }
        }

        return builder.build();
    }
}
