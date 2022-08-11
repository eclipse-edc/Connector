/*
 *  Copyright (c) 2021 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.iam.did;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.resolution.DidPublicKeyResolverImpl;
import org.eclipse.dataspaceconnector.iam.did.resolution.DidResolverRegistryImpl;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.iam.did.spi.store.DidStore;
import org.eclipse.dataspaceconnector.iam.did.store.InMemoryDidDocumentStore;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.time.Clock;


@Provides({ DidResolverRegistry.class, DidPublicKeyResolver.class })
public class IdentityDidCoreExtension implements ServiceExtension {

    @Inject
    private PrivateKeyResolver privateKeyResolver;

    @Inject
    private Clock clock;

    @Override
    public String name() {
        return "Identity Did Core";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var didResolverRegistry = new DidResolverRegistryImpl();
        context.registerService(DidResolverRegistry.class, didResolverRegistry);

        var publicKeyResolver = new DidPublicKeyResolverImpl(didResolverRegistry);
        context.registerService(DidPublicKeyResolver.class, publicKeyResolver);

        registerParsers(privateKeyResolver);
    }

    @Provider(isDefault = true)
    public DidStore defaultDidDocumentStore() {
        return new InMemoryDidDocumentStore(clock);
    }

    private void registerParsers(PrivateKeyResolver resolver) {

        // add EC-/PEM-Parser
        resolver.addParser(ECKey.class, encoded -> {
            try {
                return (ECKey) JWK.parseFromPEMEncodedObjects(encoded);
            } catch (JOSEException e) {
                throw new EdcException(e);
            }
        });
        resolver.addParser(PrivateKeyWrapper.class, encoded -> {
            try {
                var ecKey = (ECKey) JWK.parseFromPEMEncodedObjects(encoded);
                return new EcPrivateKeyWrapper(ecKey);
            } catch (JOSEException e) {
                throw new EdcException(e);
            }
        });

    }
}
