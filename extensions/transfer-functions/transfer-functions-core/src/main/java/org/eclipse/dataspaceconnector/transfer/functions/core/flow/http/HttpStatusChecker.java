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
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusChecker;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Supplier;

import static java.lang.String.format;

/**
 * Delegates to the transfer function to determine if a transfer process has completed.
 */
public class HttpStatusChecker implements StatusChecker {
    private final String checkEndpoint;
    private final Supplier<HttpClient> clientSupplier;
    private final TypeManager typeManager;
    private final Monitor monitor;

    public HttpStatusChecker(HttpFunctionConfiguration configuration) {
        this.checkEndpoint = configuration.getCheckEndpoint();
        this.clientSupplier = configuration.getClientSupplier();
        this.typeManager = configuration.getTypeManager();
        this.monitor = configuration.getMonitor();
    }

    @Override
    public boolean isComplete(TransferProcess transferProcess, List<ProvisionedResource> resources) {
        try {
            var request = HttpRequest.newBuilder(new URI(checkEndpoint)).GET().build();

            var response = clientSupplier.get().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                monitor.severe(format("Transfer function %s returned an error: %s", checkEndpoint, response.statusCode()));
                return false;
            }
            var body = response.body();
            if (body == null) {
                monitor.severe(format("Transfer function %s returned an empty response body: %s", checkEndpoint, response.statusCode()));
                return false;
            }
            return typeManager.readValue(body, Boolean.TYPE);
        } catch (IOException | URISyntaxException | InterruptedException e) {
            monitor.severe("Error invoking transfer function", e);
            return false;
        }
    }
}
