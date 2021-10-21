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
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

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
        var resolver = new WebDidResolver(httpClient, mapper, monitor);

        var resolverRegistry = context.getService(DidResolverRegistry.class);
        resolverRegistry.register(resolver);

        monitor.info("Initialized Web DID extension");
    }
}
