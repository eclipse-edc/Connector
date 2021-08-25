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

import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.resolver.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolver.DidResolver;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

/**
 *
 */
public class DidServiceExtension implements ServiceExtension {
    // TODO HACKATHON-1 Configure the connector did in local and cloud deployments
    @EdcSetting
    private static final String DID_KEY = "edc.did";

    @Override
    public Set<String> provides() {
        return Set.of("iam");
    }

    @Override
    public Set<String> requires() {
        return Set.of("identity-did-core", "identity-did-verifier");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var did = context.getSetting(DID_KEY, null);
        var monitor = context.getMonitor();
        if (did == null) {
            monitor.info("DID was not set, defaulting to: " + "did:ion:123abc");
        }
        var didResolver = context.getService(DidResolver.class);
        var publicKeyResolver = context.getService(DidPublicKeyResolver.class);
        var privateKeyResolver = context.getService(PrivateKeyResolver.class);

        var credentialsVerifier = context.getService(CredentialsVerifier.class);

        var identityService = new DistributedIdentityService(did, credentialsVerifier, didResolver, publicKeyResolver, privateKeyResolver, monitor);

        context.registerService(IdentityService.class, identityService);

        monitor.info("Initialized Distributed Identity Service extension");
    }
}
