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
import org.eclipse.dataspaceconnector.ion.spi.IonClient;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidConstants.DID_URL_SETTING;


public class IonCredentialsVerifierExtension implements ServiceExtension {

    @Override
    public Set<String> provides() {
        return Set.of(CredentialsVerifier.FEATURE);
    }

    @Override
    public Set<String> requires() {
        return Set.of(IdentityHubClient.FEATURE, IonClient.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var hubClient = context.getService(IdentityHubClient.class);

        var didUrl = context.getSetting(DID_URL_SETTING, null);
        if (didUrl == null) {
            throw new EdcException(format("The DID Url setting '(%s)' was null!", DID_URL_SETTING));
        }

        var credentialsVerifier = new IdentityHubCredentialsVerifier(hubClient, context.getMonitor(), didUrl);
        context.registerService(CredentialsVerifier.class, credentialsVerifier);

        context.getMonitor().info("Initialized ION Credentials verifier extension");
    }
}
