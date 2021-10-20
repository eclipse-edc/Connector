/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.transfer.functions.core.flow.http;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResponse;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.function.Supplier;

import static org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus.FATAL_ERROR;

/**
 * Connects with an HTTP(S) endpoint to initiate and manage a data transfer.
 *
 * Endpoints must be idempotent. The endpoint should return HTTP code 200 to indicate the request was received. If the endpoint returns an HTTP code of 500 to 504, the request
 * will be retried; otherwise the request will be placed in the fatal error state.
 */
public class HttpFunctionDataFlowController implements DataFlowController {
    public static final String JSON = "application/json";

    private final String transferEndpoint;
    private final Set<String> protocols;
    private final Supplier<HttpClient> clientSupplier;
    private final TypeManager typeManager;
    private final Monitor monitor;

    public HttpFunctionDataFlowController(HttpFunctionConfiguration configuration) {
        this.transferEndpoint = configuration.getTransferEndpoint();
        this.protocols = configuration.getProtocols();
        this.clientSupplier = configuration.getClientSupplier();
        this.typeManager = configuration.getTypeManager();
        this.monitor = configuration.getMonitor();
    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {
        return protocols.contains(dataRequest.getDestinationType());
    }

    @Override
    public @NotNull DataFlowInitiateResponse initiateFlow(DataRequest dataRequest) {
        try {
            String requestBody = typeManager.writeValueAsString(dataRequest);
            var bodyPublisher = HttpRequest.BodyPublishers.ofString(requestBody);
            var request = HttpRequest.newBuilder(new URI(transferEndpoint))
                    .header("Content-Type", JSON)
                    .POST(bodyPublisher).build();

            var response = clientSupplier.get().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return DataFlowInitiateResponse.OK;
            } else if (response.statusCode() >= 500 && response.statusCode() <= 504) {
                return new DataFlowInitiateResponse(ERROR_RETRY, "Received error code: " + response.statusCode());
            } else {
                return new DataFlowInitiateResponse(FATAL_ERROR, "Received fatal error code: " + response.statusCode());
            }
        } catch (IOException | URISyntaxException | InterruptedException e) {
            monitor.severe("Error invoking transfer function", e);
            return new DataFlowInitiateResponse(ERROR_RETRY, e.getMessage());
        }
    }
}
