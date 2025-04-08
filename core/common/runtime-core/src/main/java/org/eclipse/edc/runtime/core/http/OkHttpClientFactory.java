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

package org.eclipse.edc.runtime.core.http;

import okhttp3.EventListener;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.SocketFactory;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;

public class OkHttpClientFactory {

    /**
     * Create an OkHttpClient instance
     *
     * @param configuration       the configuration
     * @param okHttpEventListener used to instrument OkHttp client for collecting metrics, can be null
     * @param monitor             the monitor
     * @return the OkHttpClient
     */
    @NotNull
    public static OkHttpClient create(OkHttpClientConfiguration configuration, EventListener okHttpEventListener, Monitor monitor) {
        var builder = new OkHttpClient.Builder()
                .connectTimeout(configuration.getConnectTimeout(), SECONDS)
                .readTimeout(configuration.getReadTimeout(), SECONDS);

        if (configuration.getSendBufferSize() > 0 || configuration.getReceiveBufferSize() > 0) {
            builder.socketFactory(new CustomSocketFactory(configuration.getSendBufferSize(), configuration.getReceiveBufferSize()));
        }

        ofNullable(okHttpEventListener).ifPresent(builder::eventListener);

        if (configuration.isEnforceHttps()) {
            builder.addInterceptor(new EnforceHttps());
        } else {
            monitor.info("HTTPS enforcement it not enabled, please enable it in a production environment");
        }

        return builder.build();
    }

    private static class EnforceHttps implements Interceptor {
        @NotNull
        @Override
        public Response intercept(@NotNull Chain chain) throws IOException {
            var request = chain.request();
            if (!request.isHttps()) {
                throw new EdcException(format("HTTP call to %s blocked due to HTTPS enforcement enabled", request.url()));
            }
            return chain.proceed(request);
        }
    }

    private static class CustomSocketFactory extends SocketFactory {

        private final int sendBufferSize;
        private final int receiveBufferSize;

        CustomSocketFactory(int sendBufferSize, int receiveBufferSize) {
            this.sendBufferSize = sendBufferSize;
            this.receiveBufferSize = receiveBufferSize;
        }

        @Override
        public Socket createSocket() throws IOException {
            return updateSendBufferSize(new Socket());
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return updateSendBufferSize(new Socket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return updateSendBufferSize(new Socket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return updateSendBufferSize(new Socket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return updateSendBufferSize(new Socket(address, port, localAddress, localPort));
        }

        private Socket updateSendBufferSize(Socket socket) throws IOException {
            if (receiveBufferSize > 0) {
                socket.setReceiveBufferSize(receiveBufferSize);
            }
            if (sendBufferSize > 0) {
                socket.setSendBufferSize(sendBufferSize);
            }
            return socket;
        }
    }
}
