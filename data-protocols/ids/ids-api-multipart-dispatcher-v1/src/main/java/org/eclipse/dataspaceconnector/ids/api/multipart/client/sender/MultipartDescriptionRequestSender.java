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
import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ModelClass;
import de.fraunhofer.iais.eis.ResponseMessage;
import de.fraunhofer.iais.eis.util.Util;
import okhttp3.MultipartReader;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.ids.api.multipart.client.message.MultipartDescriptionResponse;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.MetadataRequest;
import org.glassfish.jersey.media.multipart.ContentDisposition;

import static org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil.gregorianNow;

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
    protected MultipartDescriptionResponse getResponseContent(ResponseBody body) throws Exception {
        ResponseMessage header = null;
        ModelClass payload = null;
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
                    payload = objectMapper.readValue(part.body().inputStream(), ModelClass.class);
                }
            }
        }

        return MultipartDescriptionResponse.Builder.newInstance()
                .header(header)
                .payload(payload)
                .build();
    }
}
