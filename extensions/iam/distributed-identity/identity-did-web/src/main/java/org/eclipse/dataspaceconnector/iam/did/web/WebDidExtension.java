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
package org.eclipse.dataspaceconnector.iam.did.web;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.dnsoverhttps.DnsOverHttps;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.iam.did.web.resolution.WebDidResolver;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.eclipse.dataspaceconnector.iam.did.web.ConfigurationKeys.DNS_OVER_HTTPS;

/**
 * Initializes support for resolving Web DIDs.
 */
public class WebDidExtension implements ServiceExtension {

    @Override
    public Set<String> requires() {
        return Set.of(DidResolverRegistry.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var mapper = context.getTypeManager().getMapper();
        var monitor = context.getMonitor();

        OkHttpClient httpClient = getOkHttpClient(context);

        var resolver = new WebDidResolver(httpClient, mapper, monitor);

        var resolverRegistry = context.getService(DidResolverRegistry.class);
        resolverRegistry.register(resolver);

        monitor.info("Initialized Web DID extension");
    }

    private OkHttpClient getOkHttpClient(ServiceExtensionContext context) {
        var httpClient = context.getService(OkHttpClient.class);
        var dnsServer = context.getSetting(DNS_OVER_HTTPS, null);

        if (dnsServer != null) {
            // use DNS over HTTPS for name lookups
            var dns = new DnsOverHttps.Builder().client(httpClient).url(requireNonNull(HttpUrl.get(dnsServer))).includeIPv6(false).build();
            httpClient = httpClient.newBuilder().dns(dns).build();
        }
        return httpClient;
    }

}
