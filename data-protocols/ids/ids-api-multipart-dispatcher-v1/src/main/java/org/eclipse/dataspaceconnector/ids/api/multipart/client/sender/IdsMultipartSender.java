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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.TokenFormat;
import jakarta.ws.rs.core.MediaType;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.MultipartReader;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.ids.api.multipart.client.message.IdsMultipartParts;
import org.eclipse.dataspaceconnector.ids.core.message.FutureCallback;
import org.eclipse.dataspaceconnector.ids.core.message.IdsMessageSender;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;
import org.glassfish.jersey.media.multipart.ContentDisposition;

public abstract class IdsMultipartSender<M extends RemoteMessage, R> implements IdsMessageSender<M, R> {

    protected static final String VERSION = "4.0.0";
    protected final URI connectorId;
    protected final OkHttpClient httpClient;
    protected final ObjectMapper objectMapper;
    protected final Monitor monitor;
    protected final IdentityService identityService;

    protected IdsMultipartSender(String connectorId,
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

    protected abstract String getConnectorId(M request);

    protected abstract String getConnectorAddress(M request);

    protected abstract Message buildMessageHeader(M request, DynamicAttributeToken token) throws Exception;

    protected String buildMessagePayload(M request) throws Exception {
        return null;
    }

    protected abstract R getResponseContent(IdsMultipartParts parts) throws Exception;

    @Override
    public CompletableFuture<R> send(M request, MessageContext context) {
        // Get connector ID
        var recipientConnectorId = getConnectorId(request);

        // Get Dynamic Attribute Token
        var tokenResult = identityService.obtainClientCredentials(recipientConnectorId);
        var token = new DynamicAttributeTokenBuilder()
                ._tokenFormat_(TokenFormat.JWT)
                ._tokenValue_(tokenResult.getToken())
                .build();

        // Initialize future
        CompletableFuture<R> future = new CompletableFuture<>();

        // Get recipient address
        var connectorAddress = getConnectorAddress(request);
        var requestUrl = HttpUrl.parse(connectorAddress);
        if (requestUrl == null) {
            future.completeExceptionally(new IllegalArgumentException("Connector address not specified"));
            return future;
        }

        // Build IDS message header
        Message message;
        try {
            message = buildMessageHeader(request, token);
        } catch (Exception e) {
            future.completeExceptionally(e);
            return future;
        }

        // Build multipart header part
        var headerPartHeaders = new Headers.Builder()
                .add("Content-Disposition", "form-data; name=\"header\"")
                .build();

        RequestBody headerRequestBody;
        try {
            headerRequestBody = RequestBody.create(
                    objectMapper.writeValueAsString(message),
                    okhttp3.MediaType.get(MediaType.APPLICATION_JSON));
        } catch (IOException exception) {
            future.completeExceptionally(exception);
            return future;
        }

        var headerPart = MultipartBody.Part.create(headerPartHeaders, headerRequestBody);

        // Build IDS message payload
        String payload;
        try {
            payload = buildMessagePayload(request);
        } catch (Exception e) {
            future.completeExceptionally(e);
            return future;
        }

        // Build multipart payload part
        MultipartBody.Part payloadPart = null;
        if (payload != null) {
            var payloadRequestBody= RequestBody.create(payload,
                    okhttp3.MediaType.get(MediaType.APPLICATION_JSON));

            var payloadPartHeaders = new Headers.Builder()
                    .add("Content-Disposition", "form-data; name=\"payload\"")
                    .build();

            payloadPart = MultipartBody.Part.create(payloadPartHeaders, payloadRequestBody);
        }

        // Build multipart body
        var multipartBuilder = new MultipartBody.Builder()
                .setType(okhttp3.MediaType.get(MediaType.MULTIPART_FORM_DATA))
                .addPart(headerPart);

        if (payloadPart != null) {
            multipartBuilder.addPart(payloadPart);
        }

        var multipartRequestBody = multipartBuilder.build();

        // Build HTTP request
        var httpRequest = new Request.Builder()
                .url(requestUrl)
                .addHeader("Content-Type", MediaType.MULTIPART_FORM_DATA)
                .post(multipartRequestBody)
                .build();

        // Execute call
        httpClient.newCall(httpRequest).enqueue(new FutureCallback<>(future, r -> {
            try (r) {
                if (r.isSuccessful()) {
                    monitor.debug("Response received from connector");
                    try (var body = r.body()) {
                        if (body == null) {
                            future.completeExceptionally(new EdcException("Received an empty body response from connector"));
                        } else {
                            IdsMultipartParts parts = extractResponseParts(body);
                            return getResponseContent(parts);
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

    private IdsMultipartParts extractResponseParts(ResponseBody body) throws Exception {
        InputStream header = null;
        InputStream payload = null;
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
                    header = new ByteArrayInputStream(part.body().readByteArray());
                } else if ("payload".equalsIgnoreCase(multipartName)) {
                    payload = new ByteArrayInputStream(part.body().readByteArray());
                }
            }
        }

        return IdsMultipartParts.Builder.newInstance()
                .header(header)
                .payload(payload)
                .build();
    }

}
