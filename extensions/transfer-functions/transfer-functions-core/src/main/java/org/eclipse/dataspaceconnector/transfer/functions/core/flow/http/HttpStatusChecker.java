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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusChecker;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import static java.lang.String.format;

/**
 * Delegates to the transfer function to determine if a transfer process has completed.
 */
public class HttpStatusChecker implements StatusChecker {
    private String checkEndpoint;
    private Supplier<OkHttpClient> clientSupplier;
    private TypeManager typeManager;
    private Monitor monitor;

    public HttpStatusChecker(HttpDataFlowConfiguration configuration) {
        this.checkEndpoint = configuration.getCheckEndpoint();
        this.clientSupplier = configuration.getClientSupplier();
        this.typeManager = configuration.getTypeManager();
        this.monitor = configuration.getMonitor();
    }

    @Override
    public boolean isComplete(TransferProcess transferProcess, List<ProvisionedResource> resources) {
        var request = new Request.Builder().url(checkEndpoint).get().build();
        try (var response = clientSupplier.get().newCall(request).execute()) {
            if (response.code() != 200) {
                monitor.severe(format("Transfer function %s returned an error: %s", checkEndpoint, response.code()));
                return false;
            }
            var body = response.body();
            if (body == null) {
                monitor.severe(format("Transfer function %s returned an empty response body: %s", checkEndpoint, response.code()));
                return false;
            }
            return typeManager.readValue(body.string(), Boolean.TYPE);
        } catch (IOException e) {
            monitor.severe("Error invoking transfer function", e);
            return false;
        }
    }
}
