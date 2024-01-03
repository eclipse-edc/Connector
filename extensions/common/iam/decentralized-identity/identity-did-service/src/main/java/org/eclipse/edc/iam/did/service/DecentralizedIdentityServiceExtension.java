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

package org.eclipse.edc.iam.did.service;

import org.eclipse.edc.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.security.PrivateKey;
import java.time.Clock;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.eclipse.edc.iam.did.spi.document.DidConstants.DID_URL_SETTING;

@Provides(IdentityService.class)
@Extension(value = DecentralizedIdentityServiceExtension.NAME)
public class DecentralizedIdentityServiceExtension implements ServiceExtension {

    public static final String NAME = "Distributed Identity Service";
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
        return NAME;
    }

    @Provider
    public IdentityService identityService(ServiceExtensionContext context) {
        var didUrl = context.getSetting(DID_URL_SETTING, null);
        if (didUrl == null) {
            throw new EdcException(format("The DID Url setting '(%s)' was null!", DID_URL_SETTING));
        }

        // we'll use the connector name to restore the Private Key
        var connectorName = context.getConnectorId();
        Supplier<PrivateKey> supplier = () -> privateKeyResolver.resolvePrivateKey(connectorName)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));

        return new DecentralizedIdentityService(resolverRegistry, credentialsVerifier, context.getMonitor(), supplier, didUrl, clock);
    }
}
