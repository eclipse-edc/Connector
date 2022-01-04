/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.core.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.ArtifactRequestMessageBuilder;
import de.fraunhofer.iais.eis.ArtifactResponseMessage;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.TokenFormat;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.eclipse.dataspaceconnector.ids.core.message.MessageFunctions.writeJson;

/**
 * Binds and sends {@link DataRequest} messages through the IDS protocol.
 */
public class DataRequestMessageSender implements IdsMessageSender<DataRequest, Object> {
    private static final String JSON = "application/json";
    private static final String VERSION = "1.0";
    private final Monitor monitor;
    private final TransferProcessManager transferProcessManager;
    private final URI connectorId;
    private final IdentityService identityService;
    private final Vault vault;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;

    public DataRequestMessageSender(String connectorId,
                                    IdentityService identityService,
                                    Vault vault,
                                    OkHttpClient httpClient,
                                    ObjectMapper mapper,
                                    Monitor monitor,
                                    TransferProcessManager transferProcessManager) {
        this.connectorId = URI.create(connectorId);
        this.identityService = identityService;
        this.vault = vault;
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.monitor = monitor;
        this.transferProcessManager = transferProcessManager;
    }

    @Override
    public Class<DataRequest> messageType() {
        return DataRequest.class;
    }

    @Override
    public CompletableFuture<Object> send(@NotNull DataRequest dataRequest, @NotNull MessageContext context) {
        Objects.requireNonNull(dataRequest, "dataRequest");
        Objects.requireNonNull(context, "messageContext");

        var connectorId = dataRequest.getConnectorId();

        var tokenResult = identityService.obtainClientCredentials(connectorId);
        if (tokenResult.failed()) {
            return failedFuture(new EdcException("Failed to obtain client credentials: " + String.join(", ", tokenResult.getFailureMessages())));
        }

        DynamicAttributeToken token = new DynamicAttributeTokenBuilder()
                ._tokenFormat_(TokenFormat.JWT)._tokenValue_(tokenResult.getContent().getToken())
                .build();

        var artifactMessage = new ArtifactRequestMessageBuilder()
                // FIXME handle timezone issue ._issued_(gregorianNow())
                ._modelVersion_(DataRequestMessageSender.VERSION)
                ._securityToken_(token)
                ._issuerConnector_(this.connectorId)
                ._requestedArtifact_(URI.create(dataRequest.getAssetId()))
                .build();
        artifactMessage.setProperty("dataspaceconnector-data-destination", dataRequest.getDataDestination());

        artifactMessage.setProperty("dataspaceconnector-properties", dataRequest.getProperties());

        var processId = context.getProcessId();

        var keyName = dataRequest.getDataDestination().getKeyName();
        // there might not be a keyname, e.g. with PULL style data transfers
        if (keyName != null) {
            var serializedToken = vault.resolveSecret(keyName);
            if (serializedToken != null) {
                artifactMessage.setProperty("dataspaceconnector-destination-token", serializedToken);
            }
        }
        artifactMessage.setProperty("dataspaceconnector-is-synch-request", dataRequest.isSync());

        var requestBody = writeJson(artifactMessage, mapper);


        var connectorAddress = dataRequest.getConnectorAddress();
        HttpUrl baseUrl = HttpUrl.parse(connectorAddress);
        if (baseUrl == null) {
            transferProcessManager.transitionError(context.getProcessId(), "Invalid connector address: " + connectorAddress);
            return CompletableFuture.completedFuture(null);
        }

        HttpUrl connectorEndpoint = baseUrl.newBuilder().addPathSegment("api").addPathSegment("ids").addPathSegment("request").build();

        Request request = new Request.Builder().url(connectorEndpoint).addHeader("Content-Type", DataRequestMessageSender.JSON).post(requestBody).build();

        CompletableFuture<Object> future = new CompletableFuture<>();

        httpClient.newCall(request).enqueue(new FutureCallback<>(future, r -> {
            try (r) {
                // TODO: future is not always completed, this
                if (r.isSuccessful()) {
                    monitor.debug("Request approved and acknowledged for process: " + processId);
                    transferProcessManager.transitionRequestAck(processId);
                    Object dataObject = extractArtifactResponse(r);
                    future.complete(dataObject);
                } else if (r.code() == 500) {
                    transferProcessManager.transitionProvisioned(processId);
                    future.completeExceptionally(new EdcException("Received InternalServerError " + r.message()));
                } else {
                    if (r.code() == 403) {
                        // forbidden
                        monitor.severe("Received not authorized from connector for process: " + processId);
                    }
                    // Fatal error
                    var rejectionMsg = extractRejectionMessage(r);
                    String message = rejectionMsg != null ?
                            String.format("IDS Rejection: '%s', code %s", rejectionMsg.getRejectionReason().name(), r.code()) :
                            "General error, HTTP response code: " + r.code();
                    monitor.info(message);
                    transferProcessManager.transitionError(processId, "General error, HTTP response code: " + r.code());
                    future.completeExceptionally(new EdcException("Received Error " + r.code() + " " + message));
                }

                return null;
            }
        }));
        return future;
    }

    private RejectionMessage extractRejectionMessage(Response r) {
        try {
            return mapper.readValue(r.body().string(), RejectionMessage.class);
        } catch (IOException ex) {
            monitor.severe("Could not read body of response", ex);
            return null;
        }
    }

    private Object extractArtifactResponse(Response httpResponse) {
        try {
            var body = httpResponse.body().string();
            var artifactResponse = mapper.readValue(body, ArtifactResponseMessage.class);
            return artifactResponse.getProperties().get("dataspaceconnector-data-object");
        } catch (IOException e) {
            monitor.severe("Could not read body of response", e);
            return null;
        }
    }

}
