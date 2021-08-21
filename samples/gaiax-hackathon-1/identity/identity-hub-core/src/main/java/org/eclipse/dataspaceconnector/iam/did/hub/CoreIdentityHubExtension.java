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
package org.eclipse.dataspaceconnector.iam.did.hub;

import org.eclipse.dataspaceconnector.iam.did.hub.spi.IdentityHub;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.IdentityHubStore;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

/**
 *
 */
public class CoreIdentityHubExtension implements ServiceExtension {
    @EdcSetting
    private static final String PRIVATE_KEY_ALIAS = "dataspaceconnector.did.private.key.alias";

    @Override
    public Set<String> requires() {
        return Set.of("identity-hub-store");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var privateKeyAlias = context.getSetting(PRIVATE_KEY_ALIAS, "privateKeyAlias");
        var privateKeyResolver = context.getService(PrivateKeyResolver.class);

        var hubStore = context.getService(IdentityHubStore.class);

        var objectMapper = context.getTypeManager().getMapper();

        var hub = new IdentityHubImpl(hubStore, () -> privateKeyResolver.resolvePrivateKey(privateKeyAlias), objectMapper);
        context.registerService(IdentityHub.class, hub);

        var controller = new IdentityHubController(hub);
        var webService = context.getService(WebService.class);
        webService.registerController(controller);

        context.getMonitor().info("Initialized Core Identity Hub extension");
    }

}
