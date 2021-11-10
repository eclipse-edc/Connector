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

package org.eclipse.dataspaceconnector.ids.core.message;

import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.TokenFormat;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.iais.eis.util.Util;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.DescriptionRequest;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil.gregorianNow;

public class MultipartDescriptionRequestSender implements IdsMessageSender<DescriptionRequest, String> {

    private static final String JSON = "application/json";
    private static final String VERSION = "4.0.0";
    private final URI connectorId;
    private final OkHttpClient httpClient;
    private final Serializer serializer;
    private final Monitor monitor;
    private final IdentityService identityService;

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
    public CompletableFuture<String> send(DescriptionRequest descriptionRequest,
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

        CompletableFuture<String> future = new CompletableFuture<>();

        var connectorAddress = descriptionRequest.getConnectorAddress();
        var requestUrl = HttpUrl.parse(connectorAddress);
        if (requestUrl == null) {
            future.completeExceptionally(new IllegalArgumentException("Connector address not specified"));
            return future;
        }

        MultipartBody requestBody;
        try {
            requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("header", serializer.serialize(descriptionRequestMessage))
                    .build();
        } catch (IOException exception) {
            future.completeExceptionally(new IOException("Failed to serialize message header", exception));
            return future;
        }

        var request = new Request.Builder()
                .url(requestUrl)
                .addHeader("Content-Type", JSON)
                .post(requestBody)
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
                            return body.string();
                        }
                    } catch (IOException e) {
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
}
