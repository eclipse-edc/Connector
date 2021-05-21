/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.core.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.spi.iam.IdentityService;
import com.microsoft.dagx.spi.message.MessageContext;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import de.fraunhofer.iais.eis.ArtifactRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.TokenFormat;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static com.microsoft.dagx.ids.core.message.MessageFunctions.writeJson;

/**
 * Binds and sends {@link DataRequest} messages through the IDS protocol.
 */
public class DataRequestMessageSender implements IdsMessageSender<DataRequest, Void> {
    private static final String JSON = "application/json";
    private static final String VERSION = "1.0";
    private final Monitor monitor;
    private final URI connectorName;
    private final IdentityService identityService;
    private final TransferProcessStore transferProcessStore;
    private final Vault vault;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;

    public DataRequestMessageSender(String connectorName,
                                    IdentityService identityService,
                                    TransferProcessStore transferProcessStore,
                                    Vault vault,
                                    OkHttpClient httpClient,
                                    ObjectMapper mapper,
                                    Monitor monitor) {
        this.connectorName = URI.create(connectorName);
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
    public CompletableFuture<Void> send(DataRequest dataRequest, MessageContext context) {
        var connectorId = dataRequest.getConnectorId();

        var tokenResult = identityService.obtainClientCredentials(connectorId);

        DynamicAttributeToken token = new DynamicAttributeTokenBuilder()._tokenFormat_(TokenFormat.JWT)._tokenValue_(tokenResult.getToken()).build();

        var artifactMessage = new ArtifactRequestMessageBuilder()
                // FIXME handle timezone issue ._issued_(gregorianNow())
                ._modelVersion_(VERSION)
                ._securityToken_(token)
                ._issuerConnector_(connectorName)
                ._requestedArtifact_(URI.create(dataRequest.getDataEntry().getId()))
                .build();
        artifactMessage.setProperty("dagx-data-destination", dataRequest.getDataDestination());

        var processId = context.getProcessId();

        var serializedToken = vault.resolveSecret(dataRequest.getDataDestination().getKeyName());
        if (serializedToken != null) {
            artifactMessage.setProperty("dagx-destination-token", serializedToken);
        }

        var requestBody = writeJson(artifactMessage, mapper);


        var connectorAddress = dataRequest.getConnectorAddress();
        HttpUrl baseUrl = HttpUrl.parse(connectorAddress);
        if (baseUrl == null) {
            return transitionToErrorState("Invalid connector address: " + connectorAddress, context);
        }

        HttpUrl connectorEndpoint = baseUrl.newBuilder().addPathSegment("api").addPathSegment("ids").addPathSegment("request").build();

        Request request = new Request.Builder().url(connectorEndpoint).addHeader("Content-Type", JSON).post(requestBody).build();

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
