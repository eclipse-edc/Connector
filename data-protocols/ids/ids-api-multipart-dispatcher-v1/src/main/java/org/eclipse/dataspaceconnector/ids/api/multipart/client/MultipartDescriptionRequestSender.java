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

package org.eclipse.dataspaceconnector.ids.api.multipart.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.BaseConnector;
import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.ModelClass;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceCatalog;
import de.fraunhofer.iais.eis.ResponseMessage;
import de.fraunhofer.iais.eis.TokenFormat;
import de.fraunhofer.iais.eis.util.Util;
import jakarta.ws.rs.core.MediaType;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.MultipartReader;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.ids.core.message.FutureCallback;
import org.eclipse.dataspaceconnector.ids.core.message.IdsMessageSender;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.MetadataRequest;
import org.glassfish.jersey.media.multipart.ContentDisposition;

import static org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil.gregorianNow;

public class MultipartDescriptionRequestSender implements IdsMessageSender<MetadataRequest, MultipartDescriptionResponse> {

    private static final String VERSION = "4.0.0";
    private final URI connectorId;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Monitor monitor;
    private final IdentityService identityService;

    public MultipartDescriptionRequestSender(String connectorId,
                                             OkHttpClient httpClient,
                                             ObjectMapper objectMapper,
                                             Monitor monitor,
                                             IdentityService identityService) {
        this.connectorId = URI.create(connectorId);
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.monitor = monitor;
        this.identityService = identityService;
    }

    @Override
    public Class<MetadataRequest> messageType() {
        return MetadataRequest.class;
    }

    @Override
    public CompletableFuture<MultipartDescriptionResponse> send(MetadataRequest metadataRequest,
                                                                MessageContext context) {
        var recipientConnectorId = metadataRequest.getConnectorId();
        var tokenResult = identityService.obtainClientCredentials(recipientConnectorId);
        var token = new DynamicAttributeTokenBuilder()
                ._tokenFormat_(TokenFormat.JWT)
                ._tokenValue_(tokenResult.getToken())
                .build();

        var descriptionRequestMessage = new DescriptionRequestMessageBuilder()
                ._modelVersion_(VERSION)
                ._issued_(gregorianNow())
                ._securityToken_(token)
                ._issuerConnector_(this.connectorId)
                ._senderAgent_(this.connectorId)
                ._recipientConnector_(Util.asList(URI.create(recipientConnectorId)))
                ._requestedElement_(metadataRequest.getRequestedAsset())
                .build();

        CompletableFuture<MultipartDescriptionResponse> future = new CompletableFuture<>();

        var connectorAddress = metadataRequest.getConnectorAddress();
        var requestUrl = HttpUrl.parse(connectorAddress);
        if (requestUrl == null) {
            future.completeExceptionally(new IllegalArgumentException("Connector address not specified"));
            return future;
        }

        var headers = new Headers.Builder()
                .add("Content-Disposition", "form-data; name=\"header\"")
                .build();

        RequestBody requestBody;
        try {
            requestBody = RequestBody.create(
                    objectMapper.writeValueAsString(descriptionRequestMessage),
                    okhttp3.MediaType.get(MediaType.APPLICATION_JSON));
        } catch (IOException exception) {
            future.completeExceptionally(exception);
            return future;
        }

        var headerPart = MultipartBody.Part.create(headers, requestBody);
        MultipartBody multipartRequestBody = new MultipartBody.Builder()
                .setType(okhttp3.MediaType.get(MediaType.MULTIPART_FORM_DATA))
                .addPart(headerPart)
                .build();

        var request = new Request.Builder()
                .url(requestUrl)
                .addHeader("Content-Type", MediaType.MULTIPART_FORM_DATA)
                .post(multipartRequestBody)
                .build();

        httpClient.newCall(request).enqueue(new FutureCallback<>(future, r -> {
            try (r) {
                if (r.isSuccessful()) {
                    monitor.debug("Description response received");
                    try (var body = r.body()) {
                        if (body == null) {
                            future.completeExceptionally(new EdcException("Received an empty body response from connector"));
                        } else {
                            return parsePayload(body);
                        }
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                } else {
                    if (r.code() == 403) {
                        // forbidden
                        future.completeExceptionally(new EdcException("Received not authorized from connector"));
                    } else {
                        future.completeExceptionally(new EdcException("Received an error from connector:" + r.code()));
                    }
                }
                return null;
            }
        }));
        return future;
    }

    private MultipartDescriptionResponse parsePayload(final ResponseBody body) throws Exception {
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
                    var headerString = new String(part.body().readByteArray(), StandardCharsets.UTF_8);
                    header = objectMapper.readValue(headerString, ResponseMessage.class);
                } else if ("payload".equalsIgnoreCase(multipartName)) {
                    var payloadString = new String(part.body().readByteArray(), StandardCharsets.UTF_8);
                    var payloadJson = objectMapper.readTree(payloadString);
                    var type = payloadJson.get("@type");
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
                        default: throw new EdcException("Unknown type");
                    }
                }
            }
        }

        return MultipartDescriptionResponse.Builder.newInstance()
                .header(header)
                .payload(payload)
                .build();
    }
}
