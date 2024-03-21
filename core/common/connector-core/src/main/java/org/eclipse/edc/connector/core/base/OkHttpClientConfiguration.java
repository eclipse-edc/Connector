/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.core.base;

public class OkHttpClientConfiguration {

    private boolean enforceHttps;
    private int connectTimeout;
    private int readTimeout;
    private int sendBufferSize;
    private int receiveBufferSize;

    private OkHttpClientConfiguration() {
    }

    public boolean isEnforceHttps() {
        return enforceHttps;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public int getSendBufferSize() {
        return sendBufferSize;
    }

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public static class Builder {

        private final OkHttpClientConfiguration instance = new OkHttpClientConfiguration();

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
        }

        public Builder enforceHttps(boolean enforceHttps) {
            instance.enforceHttps = enforceHttps;
            return this;
        }

        public Builder connectTimeout(int connectTimeout) {
            instance.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(int readTimeout) {
            instance.readTimeout = readTimeout;
            return this;
        }

        public Builder sendBufferSize(int sendBufferSize) {
            instance.sendBufferSize = sendBufferSize;
            return this;
        }

        public Builder receiveBufferSize(int receiveBufferSize) {
            instance.receiveBufferSize = receiveBufferSize;
            return this;
        }

        public OkHttpClientConfiguration build() {
            return instance;
        }
    }
}
