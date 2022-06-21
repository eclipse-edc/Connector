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
 *
 */

package org.eclipse.dataspaceconnector.identity;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.crypto.credentials.VerifiableCredentialFactory;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

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

    @Inject
    private Clock clock;

    @Override
    public String name() {
        return "Distributed Identity Service";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var vcProvider = createSupplier(context);
        var identityService = new DecentralizedIdentityService(vcProvider, resolverRegistry, credentialsVerifier, context.getMonitor(), context.getClock());
        context.registerService(IdentityService.class, identityService);
    }

    @Override
    public void start() {
        ServiceExtension.super.start();
    }

    Supplier<SignedJWT> createSupplier(ServiceExtensionContext context) {
        var didUrl = context.getSetting(DID_URL_SETTING, null);
        if (didUrl == null) {
            throw new EdcException(format("The DID Url setting '(%s)' was null!", DID_URL_SETTING));
        }

        return () -> {
            // we'll use the connector name to restore the Private Key
            var connectorName = context.getConnectorId();
            var privateKeyString = privateKeyResolver.resolvePrivateKey(connectorName, ECKey.class); //to get the private key
            Objects.requireNonNull(privateKeyString, "Couldn't resolve private key for " + connectorName);

            // we cannot store the VerifiableCredential in the Vault, because it has an expiry date
            // the Issuer claim must contain the DID URL
            return VerifiableCredentialFactory.create(privateKeyString, Map.of(VerifiableCredentialFactory.OWNER_CLAIM, connectorName), didUrl, clock);
        };
    }
}
