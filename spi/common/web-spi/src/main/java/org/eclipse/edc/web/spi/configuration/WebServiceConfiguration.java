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

/**
 * WebService configuration returned from {@link WebServiceConfigurer}
 *
 * @deprecated please use {@link PortMapping}.
 */
@Deprecated(since = "0.11.0")
public class WebServiceConfiguration {

    private Integer port;
    private String path;

    private WebServiceConfiguration() {
    }

    public String getPath() {
        return path;
    }

    public Integer getPort() {
        return port;
    }

    public static class Builder {
        private final WebServiceConfiguration config;

        private Builder() {
            config = new WebServiceConfiguration();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder path(String path) {
            config.path = path;
            return this;
        }

        public Builder port(int port) {
            config.port = port;
            return this;
        }

        public WebServiceConfiguration build() {
            Objects.requireNonNull(config.path);
            Objects.requireNonNull(config.port);
            return config;
        }
    }
}
