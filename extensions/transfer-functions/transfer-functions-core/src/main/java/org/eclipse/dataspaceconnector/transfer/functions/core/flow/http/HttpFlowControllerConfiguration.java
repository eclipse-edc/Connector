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
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Configuration for the HTTP data flow controller.
 */
public class HttpFlowControllerConfiguration {
    private String url;
    private Set<String> protocols;
    private Supplier<OkHttpClient> clientSupplier;
    private TypeManager typeManager;
    private Monitor monitor;

    public String getUrl() {
        return url;
    }

    public Set<String> getProtocols() {
        return protocols;
    }

    public Supplier<OkHttpClient> getClientSupplier() {
        return clientSupplier;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public TypeManager getTypeManager() {
        return typeManager;
    }

    private HttpFlowControllerConfiguration() {
    }

    public static class Builder {
        private HttpFlowControllerConfiguration configuration;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder url(String url) {
            configuration.url = url;
            return this;
        }

        public Builder protocols(Set<String> protocols) {
            configuration.protocols = protocols;
            return this;
        }

        public Builder clientSupplier(Supplier<OkHttpClient> clientSupplier) {
            configuration.clientSupplier = clientSupplier;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            configuration.monitor = monitor;
            return this;
        }

        public Builder typeManager(TypeManager typeManager) {
            configuration.typeManager = typeManager;
            return this;
        }

        public HttpFlowControllerConfiguration build() {
            return configuration;
        }

        private Builder() {
            configuration = new HttpFlowControllerConfiguration();
        }
    }
}
