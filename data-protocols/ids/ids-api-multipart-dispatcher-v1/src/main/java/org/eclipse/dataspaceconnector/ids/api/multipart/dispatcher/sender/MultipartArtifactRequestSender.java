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
import de.fraunhofer.iais.eis.ArtifactRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message.MultipartRequestInProcessResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;

/**
 * IdsMultipartSender implementation for data requests. Sends IDS ArtifactRequestMessages and
 * expects an IDS RequestInProcessMessage as the response.
 */
public class MultipartArtifactRequestSender extends IdsMultipartSender<DataRequest, MultipartRequestInProcessResponse> {

    public MultipartArtifactRequestSender(@NotNull String connectorId,
                                          @NotNull OkHttpClient httpClient,
                                          @NotNull ObjectMapper objectMapper,
                                          @NotNull Monitor monitor,
                                          @NotNull IdentityService identityService,
                                          @NotNull TransformerRegistry transformerRegistry) {
        super(connectorId, httpClient, objectMapper, monitor, identityService, transformerRegistry);
    }

    @Override
    public Class<DataRequest> messageType() {
        return DataRequest.class;
    }

    @Override
    protected String retrieveRemoteConnectorId(DataRequest request) {
        return request.getConnectorId();
    }

    @Override
    protected String retrieveRemoteConnectorAddress(DataRequest request) {
        return request.getConnectorAddress();
    }

    @Override
    protected Message buildMessageHeader(DataRequest request, DynamicAttributeToken token) {
        var asset = request.getAsset();
        IdsId id = IdsId.Builder.newInstance()
                .value(asset.getId())
                .type(IdsType.ARTIFACT)
                .build();
        var transformationResult = getTransformerRegistry().transform(id, URI.class);
        if (transformationResult.hasProblems()) {
            throw new EdcException("Failed to create artifact ID from asset.");
        }

        var artifactId = transformationResult.getOutput();

        return new ArtifactRequestMessageBuilder()
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                //._issued_(gregorianNow()) TODO once https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/236 is done
                ._securityToken_(token)
                ._issuerConnector_(getConnectorId())
                ._senderAgent_(getConnectorId())
                ._recipientConnector_(Collections.singletonList(URI.create(request.getConnectorId())))
                ._requestedArtifact_(artifactId)
                .build();
    }

    @Override
    protected MultipartRequestInProcessResponse getResponseContent(IdsMultipartParts parts) throws Exception {
        Message header = getObjectMapper().readValue(parts.getHeader(), Message.class);
        String payload = null;
        if (parts.getPayload() != null) {
            payload = new String(parts.getPayload().readAllBytes());
        }

        return MultipartRequestInProcessResponse.Builder.newInstance()
                .header(header)
                .payload(payload)
                .build();
    }
}
