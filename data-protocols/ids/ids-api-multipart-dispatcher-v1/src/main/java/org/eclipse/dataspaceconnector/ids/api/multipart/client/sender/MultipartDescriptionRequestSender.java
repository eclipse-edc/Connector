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

package org.eclipse.dataspaceconnector.ids.api.multipart.client.sender;

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
import de.fraunhofer.iais.eis.util.Util;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.api.multipart.client.message.IdsMultipartParts;
import org.eclipse.dataspaceconnector.ids.api.multipart.client.message.MultipartDescriptionResponse;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.MetadataRequest;

import java.net.URI;

public class MultipartDescriptionRequestSender extends IdsMultipartSender<MetadataRequest, MultipartDescriptionResponse> {

    public MultipartDescriptionRequestSender(String connectorId,
                                             OkHttpClient httpClient,
                                             ObjectMapper objectMapper,
                                             Monitor monitor,
                                             IdentityService identityService) {
        super(connectorId, httpClient, objectMapper, monitor, identityService);
    }

    @Override
    public Class<MetadataRequest> messageType() {
        return MetadataRequest.class;
    }

    @Override
    protected String getConnectorId(MetadataRequest request) {
        return request.getConnectorId();
    }

    @Override
    protected String getConnectorAddress(MetadataRequest request) {
        return request.getConnectorAddress();
    }

    @Override
    protected Message buildMessageHeader(MetadataRequest request, DynamicAttributeToken token) {
        return new DescriptionRequestMessageBuilder()
                ._modelVersion_(VERSION)
                //._issued_(gregorianNow()) TODO once https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/236 is done
                ._securityToken_(token)
                ._issuerConnector_(this.connectorId)
                ._senderAgent_(this.connectorId)
                ._recipientConnector_(Util.asList(URI.create(request.getConnectorId())))
                ._requestedElement_(request.getRequestedAsset())
                .build();
    }

    @Override
    protected MultipartDescriptionResponse getResponseContent(IdsMultipartParts parts) throws Exception {
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
                    throw new EdcException("Unknown type");
            }
        }

        return MultipartDescriptionResponse.Builder.newInstance()
                .header(header)
                .payload(payload)
                .build();
    }
}
