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

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.iam.did.web.resolution.WebDidResolver;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import static java.lang.String.format;
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
        var httpClient = context.getService(OkHttpClient.class);
        var mapper = context.getTypeManager().getMapper();
        var monitor = context.getMonitor();

        var dnsServer = getDnsServerUrl(context);

        var resolver = new WebDidResolver(dnsServer, httpClient, mapper, monitor);

        var resolverRegistry = context.getService(DidResolverRegistry.class);
        resolverRegistry.register(resolver);

        monitor.info("Initialized Web DID extension");
    }

    @Nullable
    private URL getDnsServerUrl(ServiceExtensionContext context) {
        var dnsServer = context.getSetting(DNS_OVER_HTTPS, null);
        try {
            return dnsServer != null ? new URL(dnsServer) : null;
        } catch (MalformedURLException e) {
            throw new EdcException(format("Invalid value for %s: %s", DNS_OVER_HTTPS, dnsServer), e);
        }
    }
}
