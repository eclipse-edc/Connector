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
package org.eclipse.dataspaceconnector.iam.did.credentials;

import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubClient;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

/**
 *
 */
public class GaiaXCredentialsVerifierExtension implements ServiceExtension {

    @Override
    public Set<String> provides() {
        return Set.of("identity-did-verifier");
    }

    @Override
    public Set<String> requires() {
        return Set.of("identity-did-core");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var hubClient = context.getService(IdentityHubClient.class);

        var credentialsVerifier = new GaiaXCredentialsVerifier(hubClient);
        context.registerService(CredentialsVerifier.class, credentialsVerifier);

        context.getMonitor().info("GAIA-X credentials verifier extension");
    }
}
