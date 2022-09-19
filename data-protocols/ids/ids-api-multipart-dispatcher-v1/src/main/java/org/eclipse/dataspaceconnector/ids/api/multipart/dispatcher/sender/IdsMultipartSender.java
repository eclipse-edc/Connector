/*
 *  Copyright (c) 2020 - 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.response.IdsMultipartParts;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.response.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.core.message.FutureCallback;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenParameters;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpHeaders;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.failedFuture;

/**
 * Sends IDS multipart messages.
 */
public class IdsMultipartSender {
    private static final String TOKEN_SCOPE = "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL";

    private Monitor monitor;
    private OkHttpClient httpClient;
    private IdentityService identityService;
    private ObjectMapper objectMapper;

    public IdsMultipartSender(@NotNull Monitor monitor, @NotNull OkHttpClient httpClient,
                              @NotNull IdentityService identityService,
                              @NotNull ObjectMapper objectMapper) {
        this.monitor = Objects.requireNonNull(monitor);
        this.httpClient = Objects.requireNonNull(httpClient);
        this.identityService = Objects.requireNonNull(identityService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    /**
     * Builds and sends an IDS multipart request. Parses the response to the output type defined
     * for the sub-class.
     *
     * @param request the request.
     * @return the response as {@link CompletableFuture}.
     */
    public <M extends RemoteMessage, R> CompletableFuture<R> send(M request, MultipartSenderDelegate<M, R> senderDelegate) {
        var remoteConnectorAddress = request.getConnectorAddress();

        // Get Dynamic Attribute Token
        var tokenParameters = TokenParameters.Builder.newInstance()
                .scope(TOKEN_SCOPE)
                .audience(remoteConnectorAddress)
                .build();
        var tokenResult = identityService.obtainClientCredentials(tokenParameters);
        if (tokenResult.failed()) {
            String message = "Failed to obtain token: " + String.join(",", tokenResult.getFailureMessages());
            monitor.severe(message);
            return failedFuture(new EdcException(message));
        }

        var token = new DynamicAttributeTokenBuilder()
                ._tokenFormat_(TokenFormat.JWT)
                ._tokenValue_(tokenResult.getContent().getToken())
                .build();


        // Get recipient address
        var requestUrl = HttpUrl.parse(remoteConnectorAddress);
        if (requestUrl == null) {
            return failedFuture(new IllegalArgumentException("Connector address not specified"));
        }

        // Build IDS message header
        Message message;
        try {
            message = senderDelegate.buildMessageHeader(request, token); // TODO set idsWebhookAddress globally?
        } catch (Exception e) {
            return failedFuture(e);
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
            return failedFuture(exception);
        }

        var headerPart = MultipartBody.Part.create(headerPartHeaders, headerRequestBody);

        // Build IDS message payload
        String payload;
        try {
            payload = senderDelegate.buildMessagePayload(request);
        } catch (Exception e) {
            return failedFuture(e);
        }

        // Build multipart payload part
        MultipartBody.Part payloadPart = null;
        if (payload != null) {
            var payloadRequestBody = RequestBody.create(payload,
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
        var future = new CompletableFuture<R>();

        httpClient.newCall(httpRequest).enqueue(new FutureCallback<>(future, r -> {
            try (r) {
                monitor.debug("Response received from connector. Status " + r.code());
                if (r.isSuccessful()) {
                    try (var body = r.body()) {
                        if (body == null) {
                            future.completeExceptionally(new EdcException("Received an empty body response from connector"));
                        } else {
                            var parts = extractResponseParts(body);
                            var response = senderDelegate.getResponseContent(parts);

                            checkResponseType(response, senderDelegate);

                            return response.getPayload();
                        }
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                } else {
                    future.completeExceptionally(new EdcException(format("Received an error from connector (%s): %s %s", requestUrl, r.code(), r.message())));
                }
                return null;
            }
        }));

        return future;
    }

    /**
     * Parses the multipart response. Extracts header and payload as input stream and wraps them
     * in a container object.
     *
     * @param body the response body.
     * @return a container holding the {@link InputStream}s for response header and payload.
     * @throws Exception if parsing the response fails.
     */
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

    private void checkResponseType(MultipartResponse<?> response, MultipartSenderDelegate<? extends RemoteMessage, ?> senderDelegate) {
        var type = senderDelegate.getAllowedResponseTypes();
        if (!type.contains(response.getHeader().getClass())) {
            throw new EdcException(String.format("Received %s but expected %s.", response.getHeader().getClass(), type));
        }
    }

}
