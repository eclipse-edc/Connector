/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *
 */

package org.eclipse.dataspaceconnector.identity;

import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Objects;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.iam.did.spi.document.DidConstants.DID_URL_SETTING;

@Provides(IdentityService.class)
public class DecentralizedIdentityServiceExtension implements ServiceExtension {

    @Inject
    private DidResolverRegistry resolverRegistry;

    @Inject
    private CredentialsVerifier credentialsVerifier;

    @Inject
    private PrivateKeyResolver privateKeyResolver;

    @Override
    public String name() {
        return "Distributed Identity Service";
    }

    @Provider
    public IdentityService identityService(ServiceExtensionContext context) {
        var didUrl = context.getSetting(DID_URL_SETTING, null);
        if (didUrl == null) {
            throw new EdcException(format("The DID Url setting '(%s)' was null!", DID_URL_SETTING));
        }

        // we'll use the connector name to restore the Private Key
        var connectorName = context.getConnectorId();
        var privateKey = privateKeyResolver.resolvePrivateKey(connectorName, PrivateKeyWrapper.class);
        Objects.requireNonNull(privateKey, "Couldn't resolve private key for " + connectorName);

        return new DecentralizedIdentityService(resolverRegistry, credentialsVerifier, context.getMonitor(), privateKey, didUrl, context.getClock());
    }
}
