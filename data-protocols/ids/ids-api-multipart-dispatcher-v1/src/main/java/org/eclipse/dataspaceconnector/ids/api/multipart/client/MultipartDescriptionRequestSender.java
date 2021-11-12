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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.BaseConnector;
import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResponseMessage;
import de.fraunhofer.iais.eis.TokenFormat;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
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
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.DescriptionRequest;
import org.glassfish.jersey.media.multipart.ContentDisposition;

import static org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil.gregorianNow;

public class MultipartDescriptionRequestSender implements IdsMessageSender<DescriptionRequest, MultipartDescriptionResponse> {

    private static final String JSON = "application/json";
    private static final String VERSION = "4.0.0";
    private final URI connectorId;
    private final OkHttpClient httpClient;
    private final Serializer serializer;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final Monitor monitor;
    private final IdentityService identityService;

    //TODO
    {
        OBJECT_MAPPER.registerModule(new JavaTimeModule()); // configure ISO 8601 time de/serialization
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); // serialize dates in ISO 8601 format
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        OBJECT_MAPPER.setDateFormat(df);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule module = new SimpleModule();
        OBJECT_MAPPER.registerModule(module);
    }

    public MultipartDescriptionRequestSender(String connectorId,
                                             OkHttpClient httpClient,
                                             Serializer serializer,
                                             Monitor monitor,
                                             IdentityService identityService) {
        this.connectorId = URI.create(connectorId);
        this.httpClient = httpClient;
        this.serializer = serializer;
        this.monitor = monitor;
        this.identityService = identityService;
    }

    @Override
    public Class<DescriptionRequest> messageType() {
        return DescriptionRequest.class;
    }

    @Override
    public CompletableFuture<MultipartDescriptionResponse> send(DescriptionRequest descriptionRequest,
                                          MessageContext context) {
        var recipientConnectorId = descriptionRequest.getConnectorId();
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
                ._requestedElement_(descriptionRequest.getRequestedElement())
                .build();

        CompletableFuture<MultipartDescriptionResponse> future = new CompletableFuture<>();

        var connectorAddress = descriptionRequest.getConnectorAddress();
        var requestUrl = HttpUrl.parse(connectorAddress);
        if (requestUrl == null) {
            future.completeExceptionally(new IllegalArgumentException("Connector address not specified"));
            return future;
        }

//        MultipartBody requestBody;
//        try {
//            requestBody = new MultipartBody.Builder()
//                    .setType(okhttp3.MediaType.get(MediaType.MULTIPART_FORM_DATA))
//                    .addFormDataPart("header", serializer.serialize(descriptionRequestMessage))
//                    .build();
//        } catch (IOException exception) {
//            future.completeExceptionally(new IOException("Failed to serialize message header", exception));
//            return future;
//        }

        Headers headers = new Headers.Builder()
                .add("Content-Disposition", "form-data; name=\"header\"")
                .build();

        RequestBody requestBody;
        try {
            requestBody = RequestBody.create(
                    OBJECT_MAPPER.writeValueAsString(descriptionRequestMessage),
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
                            //TODO should be deserialized
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
        Object payload = null;
        try (MultipartReader multipartReader = new MultipartReader(Objects.requireNonNull(body))) {
            MultipartReader.Part part;
            while ((part = multipartReader.nextPart()) != null) {
                HttpHeaders httpHeaders = HttpHeaders.of(
                        part.headers().toMultimap(),
                        (a, b) -> a.equalsIgnoreCase("Content-Disposition")
                );

                String value = httpHeaders.firstValue("Content-Disposition").orElse(null);
                if (value == null) {
                    continue;
                }

                ContentDisposition contentDisposition = new ContentDisposition(value);
                String multipartName = contentDisposition.getParameters().get("name");

                if ("header".equalsIgnoreCase(multipartName)) {
                    var headerString = new String(part.body().readByteArray(), StandardCharsets.UTF_8);
                    //header = serializer.deserialize(headerString, ResponseMessage.class);
                    header = OBJECT_MAPPER.readValue(headerString, ResponseMessage.class);
                } else if ("payload".equalsIgnoreCase(multipartName)) {
                    var payloadString = new String(part.body().readByteArray(), StandardCharsets.UTF_8);
                    var payloadJson = OBJECT_MAPPER.readTree(payloadString);
                    var type = payloadJson.get("@type");
                    switch (type.textValue()) {
                        case "ids:BaseConnector":
                            //payload = serializer.deserialize(payloadString, BaseConnector.class);
                            payload = OBJECT_MAPPER.readValue(payloadString, BaseConnector.class);
                            break;
                        case "ids:Resource":
                            payload = serializer.deserialize(payloadString, Resource.class);
                            break;
                        case "ids:Representation":
                            payload = serializer.deserialize(payloadString, Representation.class);
                            break;
                        case "ids:Artifact":
                            payload = serializer.deserialize(payloadString, Artifact.class);
                            break;
                        default: throw new EdcException("Unknown type");
                    }
                }
            }
        }

        //TODO use builder
        return new MultipartDescriptionResponse(header, payload);
    }
}
