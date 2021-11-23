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

import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.ArtifactRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ResponseMessage;
import de.fraunhofer.iais.eis.util.Util;
import okhttp3.MultipartReader;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.ids.api.multipart.client.message.MultipartRequestInProcessResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.glassfish.jersey.media.multipart.ContentDisposition;

public class MultipartArtifactRequestSender extends IdsMultipartSender<DataRequest, MultipartRequestInProcessResponse> {

    private final TransformerRegistry transformerRegistry;

    public MultipartArtifactRequestSender(String connectorId,
                                          OkHttpClient httpClient,
                                          ObjectMapper objectMapper,
                                          Monitor monitor,
                                          IdentityService identityService,
                                          TransformerRegistry transformerRegistry) {
        super(connectorId, httpClient, objectMapper, monitor, identityService);
        this.transformerRegistry = transformerRegistry;
    }

    @Override
    public Class<DataRequest> messageType() {
        return DataRequest.class;
    }

    @Override
    protected String getConnectorId(DataRequest request) {
        return request.getConnectorId();
    }

    @Override
    protected String getConnectorAddress(DataRequest request) {
        return request.getConnectorAddress();
    }

    @Override
    protected Message buildMessageHeader(DataRequest request, DynamicAttributeToken token) throws Exception {
        var asset = request.getAsset();
        IdsId id = IdsId.Builder.newInstance()
                .value(asset.getId())
                .type(IdsType.ARTIFACT)
                .build();
        var transformationResult = transformerRegistry.transform(id, URI.class);
        if (transformationResult.hasProblems()) {
            throw new EdcException("Failed to create artifact ID from asset.");
        }

        var artifactId = transformationResult.getOutput();

        return new ArtifactRequestMessageBuilder()
                ._modelVersion_(VERSION)
                //._issued_(gregorianNow()) TODO once https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/236 is done
                ._securityToken_(token)
                ._issuerConnector_(this.connectorId)
                ._senderAgent_(this.connectorId)
                ._recipientConnector_(Util.asList(URI.create(request.getConnectorId())))
                ._requestedArtifact_(artifactId)
                .build();
    }

    @Override
    protected MultipartRequestInProcessResponse getResponseContent(ResponseBody body) throws Exception {
        ResponseMessage header = null;
        String payload = null;
        try (var multipartReader = new MultipartReader(Objects.requireNonNull(body))) {
            MultipartReader.Part part;
            while ((part = multipartReader.nextPart()) != null) {
                var httpHeaders = HttpHeaders.of(
                        part.headers().toMultimap(),
                        (a, b) -> a.equalsIgnoreCase("Content-Disposition")
                );

                var value = httpHeaders.firstValue("Content-Disposition").orElse(null);
                if (value == null) {
                    continue;
                }

                var contentDisposition = new ContentDisposition(value);
                var multipartName = contentDisposition.getParameters().get("name");

                if ("header".equalsIgnoreCase(multipartName)) {
                    header = objectMapper.readValue(part.body().inputStream(), ResponseMessage.class);
                } else if ("payload".equalsIgnoreCase(multipartName)) {
                    payload = objectMapper.readValue(part.body().inputStream(), String.class);
                }
            }
        }

        return MultipartRequestInProcessResponse.Builder.newInstance()
                .header(header)
                .payload(payload)
                .build();
    }
}
