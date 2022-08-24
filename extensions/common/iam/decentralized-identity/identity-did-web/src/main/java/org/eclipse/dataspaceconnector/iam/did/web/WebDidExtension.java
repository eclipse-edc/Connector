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
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.iam.did.web.resolution.WebDidResolver;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import static java.util.Objects.requireNonNull;
import static org.eclipse.dataspaceconnector.iam.did.web.ConfigurationKeys.DNS_OVER_HTTPS;


/**
 * Initializes support for resolving Web DIDs.
 */
public class WebDidExtension implements ServiceExtension {
    /**
     * Set to {@code false} to create DID URLs with {@code http} instead of {@code https} scheme.
     * Defaults to {@code true}.
     * <p>
     * This setting can be used by EDC downstream projects, e.g. for testing with docker compose
     */
    @EdcSetting
    private static final String USE_HTTPS_SCHEME = "edc.iam.did.web.use.https";

    @Inject
    private DidResolverRegistry resolverRegistry;

    @Override
    public String name() {
        return "Web DID";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var mapper = context.getTypeManager().getMapper();
        var monitor = context.getMonitor();
        var useHttpsScheme = context.getSetting(USE_HTTPS_SCHEME, true);

        OkHttpClient httpClient = getOkHttpClient(context);

        var resolver = new WebDidResolver(httpClient, useHttpsScheme, mapper, monitor);

        resolverRegistry.register(resolver);
    }

    private OkHttpClient getOkHttpClient(ServiceExtensionContext context) {
        var httpClient = context.getService(OkHttpClient.class);
        var dnsServer = context.getSetting(DNS_OVER_HTTPS, null);

        if (!StringUtils.isNullOrEmpty(dnsServer)) {
            // use DNS over HTTPS for name lookups
            var dns = new DnsOverHttps.Builder().client(httpClient).url(requireNonNull(HttpUrl.get(dnsServer))).includeIPv6(false).build();
            httpClient = httpClient.newBuilder().dns(dns).build();
        }
        return httpClient;
    }

}
