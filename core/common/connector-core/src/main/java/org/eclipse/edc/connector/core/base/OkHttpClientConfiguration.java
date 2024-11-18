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

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;

@Settings
public class OkHttpClientConfiguration {
    public static final boolean DEFAULT_OK_HTTP_CLIENT_HTTPS_ENFORCE = false;
    public static final int DEFAULT_OK_HTTP_CLIENT_TIMEOUT_CONNECT = 30;
    public static final int DEFAULT_OK_HTTP_CLIENT_TIMEOUT_READ = 30;
    public static final int DEFAULT_OK_HTTP_CLIENT_SEND_BUFFER_SIZE = 0;
    public static final int DEFAULT_OK_HTTP_CLIENT_RECEIVE_BUFFER_SIZE = 0;

    @Setting(description = "OkHttpClient: If true, enable HTTPS call enforcement", defaultValue = DEFAULT_OK_HTTP_CLIENT_HTTPS_ENFORCE + "", key = "edc.http.client.https.enforce")
    private boolean enforceHttps;

    @Setting(description = "OkHttpClient: connect timeout, in seconds", defaultValue = DEFAULT_OK_HTTP_CLIENT_TIMEOUT_CONNECT + "", key = "edc.http.client.timeout.connect")
    private int connectTimeout;
    @Setting(description = "OkHttpClient: read timeout, in seconds", defaultValue = DEFAULT_OK_HTTP_CLIENT_TIMEOUT_READ + "", key = "edc.http.client.timeout.read")
    private int readTimeout;
    @Setting(description = "OkHttpClient: send buffer size, in bytes", defaultValue = DEFAULT_OK_HTTP_CLIENT_SEND_BUFFER_SIZE + "", key = "edc.http.client.send.buffer.size", min = 1)
    private int sendBufferSize;
    @Setting(description = "OkHttpClient: receive buffer size, in bytes", defaultValue = DEFAULT_OK_HTTP_CLIENT_RECEIVE_BUFFER_SIZE + "", key = "edc.http.client.receive.buffer.size", min = 1)
    private int receiveBufferSize;

    public OkHttpClientConfiguration() {
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

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder {

        private final OkHttpClientConfiguration instance;

        private Builder(OkHttpClientConfiguration okHttpClientConfiguration) {
            this.instance = okHttpClientConfiguration;
        }

        public static Builder newInstance() {
            return new Builder(new OkHttpClientConfiguration());
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
