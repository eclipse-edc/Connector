/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.web.spi.configuration;

import java.util.Objects;

public class WebServiceSettings {

    private String apiConfigKey;
    private Integer defaultPort;
    private String defaultPath;
    private String contextAlias;

    private WebServiceSettings() {

    }

    /**
     * Returns the key for the ws config key eg `web.http.management`
     */
    public String apiConfigKey() {
        return apiConfigKey;
    }

    /**
     * The default port if the config {@link WebServiceSettings#apiConfigKey()} is not found in {@link org.eclipse.edc.spi.system.ServiceExtensionContext#getConfig}
     */
    public Integer getDefaultPort() {
        return defaultPort;
    }

    /**
     * The default path if the config {@link WebServiceSettings#apiConfigKey()} is not found in {@link org.eclipse.edc.spi.system.ServiceExtensionContext#getConfig}
     */
    public String getDefaultPath() {
        return defaultPath;
    }

    /**
     * The name of the API context
     */
    public String getContextAlias() {
        return contextAlias;
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiConfigKey, defaultPort, defaultPath, contextAlias);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (WebServiceSettings) o;
        return apiConfigKey.equals(that.apiConfigKey) &&
                defaultPort.equals(that.defaultPort) && defaultPath.equals(that.defaultPath) &&
                Objects.equals(contextAlias, that.contextAlias);
    }

    public static class Builder {

        private final WebServiceSettings settings;

        private Builder() {
            settings = new WebServiceSettings();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder contextAlias(String contextAlias) {
            settings.contextAlias = contextAlias;
            return this;
        }

        public Builder defaultPath(String defaultPath) {
            settings.defaultPath = defaultPath;
            return this;
        }

        public Builder apiConfigKey(String apiConfigKey) {
            settings.apiConfigKey = apiConfigKey;
            return this;
        }

        public Builder defaultPort(int defaultPort) {
            settings.defaultPort = defaultPort;
            return this;
        }

        public WebServiceSettings build() {
            Objects.requireNonNull(settings.apiConfigKey);
            Objects.requireNonNull(settings.contextAlias);
            Objects.requireNonNull(settings.defaultPort);

            if (settings.defaultPath == null) {
                settings.defaultPath = "/api/" + settings.contextAlias;
            }

            return settings;
        }

    }
}
