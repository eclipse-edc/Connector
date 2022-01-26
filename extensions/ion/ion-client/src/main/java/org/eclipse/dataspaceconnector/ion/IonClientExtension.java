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
package org.eclipse.dataspaceconnector.ion;

import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.ion.spi.IonClient;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides(IonClient.class)
public class IonClientExtension implements ServiceExtension {

    private static final String ION_NODE_URL_SETTING = "edc:ion:node:url";
    private static final String DEFAULT_NODE_URL = "https://beta.discover.did.microsoft.com/1.0";
    @Inject
    private DidResolverRegistry resolverRegistry;

    @Override
    public String name() {
        return "ION Client";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        String ionEndpoint = getIonEndpoint(context);
        context.getMonitor().info("Using ION Node for resolution " + ionEndpoint);
        var client = new IonClientImpl(ionEndpoint, context.getTypeManager().getMapper());
        context.registerService(IonClient.class, client);

        resolverRegistry.register(client);
    }

    private String getIonEndpoint(ServiceExtensionContext context) {
        return context.getSetting(ION_NODE_URL_SETTING, DEFAULT_NODE_URL);
    }
}
