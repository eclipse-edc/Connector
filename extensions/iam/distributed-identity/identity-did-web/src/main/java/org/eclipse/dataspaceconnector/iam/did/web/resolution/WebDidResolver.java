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
package org.eclipse.dataspaceconnector.iam.did.web.resolution;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.dnsoverhttps.DnsOverHttps;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Resolves a Web DID according to the Web DID specification (https://w3c-ccg.github.io/did-method-web).
 */
public class WebDidResolver implements DidResolver {
    private static final String DID_METHOD = "web";
    private static final String DID_METHOD_PREFIX = DID_METHOD + ":";
    private static final String HTTPS_PREFIX = " https://";
    private static final String DID_DOCUMENT = "did.json";
    private static final String WELL_KNOWN = "/.well-known/";

    private OkHttpClient httpClient;
    private ObjectMapper mapper;
    private Monitor monitor;

    /**
     * Creates a resolver that executes DNS over HTTPs queries against the given DNS server.
     */
    public WebDidResolver(@Nullable URL dnsServer, OkHttpClient httpClient, ObjectMapper mapper, Monitor monitor) {
        if (dnsServer != null) {
            // use DNS over HTTPS for name lookups
            var dns = new DnsOverHttps.Builder().client(httpClient).url(requireNonNull(HttpUrl.get(dnsServer))).includeIPv6(false).build();
            this.httpClient = httpClient.newBuilder().dns(dns).build();
        } else {
            // use standard DNS lookups
            this.httpClient = httpClient;
        }
        this.mapper = mapper;
        this.monitor = monitor;
    }

    /**
     * Creates a resolver that executes standard DNS lookups.
     */
    public WebDidResolver(OkHttpClient httpClient, ObjectMapper mapper, Monitor monitor) {
        this(null, httpClient, mapper, monitor);
    }

    @Override
    public @NotNull String getMethod() {
        return DID_METHOD;
    }

    @Override
    @Nullable
    public DidDocument resolve(String didKey) {
        try {
            Request request = new Request.Builder().url(keyToUrl(didKey)).get().build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.code() != 200) {
                    monitor.severe(format("Error resolving DID: %s. HTTP Code was: %s", didKey, response.code()));
                    return null;
                }
                try (var body = response.body()) {
                    if (body == null) {
                        monitor.severe("DID response contained an empty body: " + didKey);
                        return null;
                    }
                    return mapper.readValue(body.string(), DidDocument.class);
                }
            } catch (IOException e) {
                monitor.severe("Error resolving DID: " + didKey, e);
                return null;
            }
        } catch (URISyntaxException | IllegalArgumentException | MalformedURLException e) {
            monitor.severe("Invalid DID key: " + didKey, e);
            return null;
        }
    }

    /**
     * Converts a DID URN to a URL according to the Web DID specification, .cf https://w3c-ccg.github.io/did-method-web/#read-resolve.
     */
    String keyToUrl(String didKey) throws URISyntaxException, IllegalArgumentException, MalformedURLException {
        var uri = new URI(didKey);
        if (!"did".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Invalid DID scheme: " + uri.getScheme());
        }
        var part = uri.getSchemeSpecificPart();

        if (!part.startsWith(DID_METHOD_PREFIX)) {
            throw new IllegalArgumentException("Invalid DID format, the URN must specify the 'web' DID Method: " + didKey);
        } else if (part.endsWith(":")) {
            throw new IllegalArgumentException("Invalid DID format, the URN must not end with ':': " + didKey);
        }
        var identifier = new URL(HTTPS_PREFIX + part.substring(DID_METHOD_PREFIX.length()).replace(':', '/'));

        if (identifier.getPath().length() == 0) {
            return identifier + WELL_KNOWN + DID_DOCUMENT;
        } else {
            return identifier + "/" + DID_DOCUMENT;
        }

    }


}
