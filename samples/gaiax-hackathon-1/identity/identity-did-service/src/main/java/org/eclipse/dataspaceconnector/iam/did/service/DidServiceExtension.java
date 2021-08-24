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
package org.eclipse.dataspaceconnector.iam.did.service;

import org.eclipse.dataspaceconnector.iam.did.spi.resolver.DidResolver;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

/**
 *
 */
public class DidServiceExtension implements ServiceExtension {

    @Override
    public Set<String> provides() {
        return Set.of("iam");
    }

    @Override
    public Set<String> requires() {
        return Set.of("identity-did-core");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var vault = context.getService(Vault.class);

        var didResolver = context.getService(DidResolver.class);

        var identityService = new DistributedIdentityService(didResolver, vault);

        context.registerService(IdentityService.class, identityService);

        context.getMonitor().info("Initialized Distributed Identity Service extension");
    }
}
