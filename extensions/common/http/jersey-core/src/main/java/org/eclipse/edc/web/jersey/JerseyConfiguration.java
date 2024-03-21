/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.web.jersey;

/**
 * Jersey extension configuration class
 */
public class JerseyConfiguration {

    private String allowedOrigins;
    private String allowedHeaders;
    private String allowedMethods;
    private boolean corsEnabled;

    private JerseyConfiguration() {
    }

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public String getAllowedHeaders() {
        return allowedHeaders;
    }

    public String getAllowedMethods() {
        return allowedMethods;
    }

    public boolean isCorsEnabled() {
        return corsEnabled;
    }

    public static class Builder {

        private final JerseyConfiguration instance = new JerseyConfiguration();

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
        }

        public Builder allowedOrigins(String allowedOrigins) {
            instance.allowedOrigins = allowedOrigins;
            return this;
        }

        public Builder allowedHeaders(String allowedHeaders) {
            instance.allowedHeaders = allowedHeaders;
            return this;
        }

        public Builder allowedMethods(String allowedMethods) {
            instance.allowedMethods = allowedMethods;
            return this;
        }

        public Builder corsEnabled(boolean corsEnabled) {
            instance.corsEnabled = corsEnabled;
            return this;
        }

        public JerseyConfiguration build() {
            return instance;
        }

    }

}
