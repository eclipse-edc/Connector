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
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.TokenFormat;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.dataspaceconnector.ids.core.message.MessageFunctions.writeJson;

/**
 * Binds and sends {@link DataRequest} messages through the IDS protocol.
 */
public class DataRequestMessageSender implements IdsMessageSender<DataRequest, Void> {
    private static final String JSON = "application/json";
    private static final String VERSION = "1.0";
    private final Monitor monitor;
    private final URI connectorId;
    private final IdentityService identityService;
    private final TransferProcessStore transferProcessStore;
    private final Vault vault;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;

    public DataRequestMessageSender(String connectorId,
                                    IdentityService identityService,
                                    TransferProcessStore transferProcessStore,
                                    Vault vault,
                                    OkHttpClient httpClient,
                                    ObjectMapper mapper,
                                    Monitor monitor) {
        this.connectorId = URI.create(connectorId);
        this.identityService = identityService;
        this.transferProcessStore = transferProcessStore;
        this.vault = vault;
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.monitor = monitor;
    }

    @Override
    public Class<DataRequest> messageType() {
        return DataRequest.class;
    }

    @Override
    public CompletableFuture<Void> send(@NotNull DataRequest dataRequest, @NotNull MessageContext context) {
        Objects.requireNonNull(dataRequest, "dataRequest");
        Objects.requireNonNull(context, "messageContext");

        var connectorId = dataRequest.getConnectorId();

        var tokenResult = identityService.obtainClientCredentials(connectorId);

        DynamicAttributeToken token = new DynamicAttributeTokenBuilder()._tokenFormat_(TokenFormat.JWT)._tokenValue_(tokenResult.getToken()).build();

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

        var serializedToken = vault.resolveSecret(dataRequest.getDataDestination().getKeyName());
        if (serializedToken != null) {
            artifactMessage.setProperty("dataspaceconnector-destination-token", serializedToken);
        }

        var requestBody = writeJson(artifactMessage, mapper);


        var connectorAddress = dataRequest.getConnectorAddress();
        HttpUrl baseUrl = HttpUrl.parse(connectorAddress);
        if (baseUrl == null) {
            return transitionToErrorState("Invalid connector address: " + connectorAddress, context);
        }

        HttpUrl connectorEndpoint = baseUrl.newBuilder().addPathSegment("api").addPathSegment("ids").addPathSegment("request").build();

        Request request = new Request.Builder().url(connectorEndpoint).addHeader("Content-Type", DataRequestMessageSender.JSON).post(requestBody).build();

        CompletableFuture<Void> future = new CompletableFuture<>();

        httpClient.newCall(request).enqueue(new FutureCallback<>(future, r -> {
            try (r) {
                TransferProcess transferProcess = transferProcessStore.find(processId);
                if (r.isSuccessful()) {
                    monitor.debug("Request approved and acknowledged for process: " + processId);
                    transferProcess.transitionRequestAck();
                } else if (r.code() == 500) {
                    transferProcess.transitionProvisioned();  // force retry
                } else {
                    if (r.code() == 403) {
                        // forbidden
                        monitor.severe("Received not authorized from connector for process: " + processId);
                    }
                    // Fatal error
                    transferProcess.transitionError("General error, HTTP response code: " + r.code());
                }
                transferProcessStore.update(transferProcess);
                return null;
            }
        }));
        return future;
    }

    @NotNull
    private CompletableFuture<Void> transitionToErrorState(String error, MessageContext context) {
        TransferProcess transferProcess = transferProcessStore.find(context.getProcessId());
        transferProcess.transitionError(error);
        transferProcessStore.update(transferProcess);
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

}
