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

package org.eclipse.edc.connector.core.base;

import okhttp3.EventListener;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;

public class OkHttpClientFactory {

    private static final String DEFAULT_TIMEOUT = "30";
    private static final String DEFAULT_HTTPS_ENFORCE = "false";

    @Setting(value = "DEPRECATED. If true, enable HTTPS call enforcement. Default value is 'false'", type = "boolean")
    @Deprecated(since = "0.1.3")
    public static final String EDC_HTTP_ENFORCE_HTTPS = "edc.http.enforce-https";

    @Setting(value = "If true, enable HTTPS call enforcement.", defaultValue = DEFAULT_HTTPS_ENFORCE, type = "boolean")
    public static final String EDC_HTTP_CLIENT_HTTPS_ENFORCE = "edc.http.client.https.enforce";

    @Setting(value = "HTTP Client connect timeout, in seconds", defaultValue = DEFAULT_TIMEOUT, type = "int")
    public static final String EDC_HTTP_CLIENT_TIMEOUT_CONNECT = "edc.http.client.timeout.connect";

    @Setting(value = "HTTP Client read timeout, in seconds", defaultValue = DEFAULT_TIMEOUT, type = "int")
    public static final String EDC_HTTP_CLIENT_TIMEOUT_READ = "edc.http.client.timeout.read";

    /**
     * Create an OkHttpClient instance
     *
     * @param context             the service extension context
     * @param okHttpEventListener used to instrument OkHttp client for collecting metrics, can be null
     * @return the OkHttpClient
     */
    @NotNull
    public static OkHttpClient create(ServiceExtensionContext context, EventListener okHttpEventListener) {
        var connectTimeout = context.getSetting(EDC_HTTP_CLIENT_TIMEOUT_CONNECT, parseInt(DEFAULT_TIMEOUT));
        var readTimeout = context.getSetting(EDC_HTTP_CLIENT_TIMEOUT_READ, parseInt(DEFAULT_TIMEOUT));

        var builder = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, SECONDS)
                .readTimeout(readTimeout, SECONDS);

        ofNullable(okHttpEventListener).ifPresent(builder::eventListener);

        if (context.getSetting(EDC_HTTP_ENFORCE_HTTPS, null) != null) {
            context.getMonitor().warning(format("Configuration setting %s has been deprecated, please use %s instead", EDC_HTTP_ENFORCE_HTTPS, EDC_HTTP_CLIENT_HTTPS_ENFORCE));
        }

        var enforceHttps = context.getSetting(EDC_HTTP_CLIENT_HTTPS_ENFORCE, context.getSetting(EDC_HTTP_ENFORCE_HTTPS, Boolean.parseBoolean(DEFAULT_HTTPS_ENFORCE)));
        if (enforceHttps) {
            builder.addInterceptor(new EnforceHttps());
        } else {
            context.getMonitor().info("HTTPS enforcement it not enabled, please enable it in a production environment");
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
}
