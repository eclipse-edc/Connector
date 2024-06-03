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

    private boolean useDefaultContext = false;
    private String apiConfigKey;
    private Integer defaultPort;
    private String defaultPath;
    private String contextAlias;
    private String name;

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

    /**
     * Returns true if the default context should be taken into consideration if there's not one context-specific
     * configured
     */
    public boolean useDefaultContext() {
        return useDefaultContext;
    }

    /**
     * The name of the API settings. It's intended only for displaying the name of the Web Extension that
     * will be configured by {@link WebServiceConfigurer}.
     */
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(useDefaultContext, apiConfigKey, defaultPort, defaultPath, contextAlias, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WebServiceSettings that = (WebServiceSettings) o;
        return useDefaultContext == that.useDefaultContext && apiConfigKey.equals(that.apiConfigKey) &&
                defaultPort.equals(that.defaultPort) && defaultPath.equals(that.defaultPath) &&
                Objects.equals(contextAlias, that.contextAlias) && name.equals(that.name);
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

        public Builder name(String name) {
            settings.name = name;
            return this;
        }

        public Builder defaultPort(int defaultPort) {
            settings.defaultPort = defaultPort;
            return this;
        }

        public Builder useDefaultContext(boolean useDefaultContext) {
            settings.useDefaultContext = useDefaultContext;
            return this;
        }

        public WebServiceSettings build() {
            Objects.requireNonNull(settings.apiConfigKey);
            Objects.requireNonNull(settings.defaultPath);
            Objects.requireNonNull(settings.defaultPort);
            Objects.requireNonNull(settings.name);

            return settings;
        }

    }
}
