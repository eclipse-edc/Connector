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
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.dataspaceconnector.ids.core.message.MessageFunctions.writeJsonPublisher;

/**
 * Binds and sends {@link DataRequest} messages through the IDS protocol.
 */
public class DataRequestMessageSender implements IdsMessageSender<DataRequest, Void> {
    private static final String JSON = "application/json";
    private static final String VERSION = "1.0";
    private final Monitor monitor;
    private final HttpClient httpClient;
    private final URI connectorId;
    private final IdentityService identityService;
    private final TransferProcessStore transferProcessStore;
    private final Vault vault;
    private final ObjectMapper mapper;

    public DataRequestMessageSender(String connectorId,
                                    IdentityService identityService,
                                    TransferProcessStore transferProcessStore,
                                    Vault vault,
                                    ObjectMapper mapper,
                                    Monitor monitor,
                                    HttpClient httpClient) {
        this.connectorId = URI.create(connectorId);
        this.identityService = identityService;
        this.transferProcessStore = transferProcessStore;
        this.vault = vault;
        this.mapper = mapper;
        this.monitor = monitor;
        this.httpClient = httpClient;
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
                ._modelVersion_(DataRequestMessageSender.VERSION)
                ._securityToken_(token)
                ._issuerConnector_(this.connectorId)
                ._requestedArtifact_(URI.create(dataRequest.getDataEntry().getId()))
                .build();
        artifactMessage.setProperty("dataspaceconnector-data-destination", dataRequest.getDataDestination());

        var processId = context.getProcessId();

        var serializedToken = vault.resolveSecret(dataRequest.getDataDestination().getKeyName());
        if (serializedToken != null) {
            artifactMessage.setProperty("dataspaceconnector-destination-token", serializedToken);
        }

        var connectorAddress = dataRequest.getConnectorAddress();
        if (connectorAddress == null) {
            return transitionToErrorState("Connector address not specified", context);
        }

        URI uri = buildUri(connectorAddress + "/api/ids/request");
        HttpRequest.BodyPublisher bodyPublisher = writeJsonPublisher(artifactMessage, mapper);

        HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                .header("Content-Type", JSON)
                .POST(bodyPublisher)
                .build();

        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    TransferProcess transferProcess = transferProcessStore.find(processId);
                    if (response.statusCode() >= 200 && response.statusCode() <= 299) {
                        monitor.debug("Request approved and acknowledged for process: " + processId);
                        transferProcess.transitionRequestAck();
                    } else if (response.statusCode() == 500) {
                        transferProcess.transitionProvisioned();  // force retry
                    } else {
                        if (response.statusCode() == 403) {
                            monitor.severe("Received not authorized from connector for process: " + processId);
                        }
                        // Fatal error
                        transferProcess.transitionError("General error, HTTP response code: " + response.statusCode());
                    }
                    transferProcessStore.update(transferProcess);
                    return null;
                });
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

    @NotNull
    private URI buildUri(String str) {
        try {
            return new URI(str);
        } catch (URISyntaxException e) {
            throw new EdcException("URI " + str + " is not valid");
        }
    }
}
