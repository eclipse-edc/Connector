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
package org.eclipse.dataspaceconnector.iam.did;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.hub.IdentityHubClientImpl;
import org.eclipse.dataspaceconnector.iam.did.hub.IdentityHubController;
import org.eclipse.dataspaceconnector.iam.did.hub.IdentityHubImpl;
import org.eclipse.dataspaceconnector.iam.did.resolution.DefaultDidPublicKeyResolver;
import org.eclipse.dataspaceconnector.iam.did.resolution.DidResolverRegistryImpl;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHub;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubClient;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubStore;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;
import java.util.function.Supplier;


public class IdentityDidCoreExtension implements ServiceExtension {

    @Override
    public Set<String> provides() {
        return Set.of(IdentityHub.FEATURE, IdentityHubClient.FEATURE, DidResolverRegistry.FEATURE);
    }

    @Override
    public Set<String> requires() {
        return Set.of(IdentityHubStore.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var hubStore = context.getService(IdentityHubStore.class);

        var objectMapper = context.getTypeManager().getMapper();

        var resolverRegistry = new DidResolverRegistryImpl();
        context.registerService(DidResolverRegistry.class, resolverRegistry);

        var publicKeyResolver = new DefaultDidPublicKeyResolver(resolverRegistry);
        context.registerService(DidPublicKeyResolver.class, publicKeyResolver);

        var privateKeyResolver = context.getService(PrivateKeyResolver.class);
        registerParsers(privateKeyResolver);

        PrivateKeyWrapper privateKeyWrapper = privateKeyResolver.resolvePrivateKey(context.getConnectorId(), PrivateKeyWrapper.class);
        Supplier<PrivateKeyWrapper> supplier = () -> privateKeyWrapper;
        var hub = new IdentityHubImpl(hubStore, supplier, publicKeyResolver, objectMapper);
        context.registerService(IdentityHub.class, hub);

        var controller = new IdentityHubController(hub);
        var webService = context.getService(WebService.class);
        webService.registerController(controller);

        var httpClient = context.getService(OkHttpClient.class);

        var hubClient = new IdentityHubClientImpl(supplier, httpClient, objectMapper);
        context.registerService(IdentityHubClient.class, hubClient);

        context.getMonitor().info("Initialized Identity Did Core extension");
    }

    private void registerParsers(PrivateKeyResolver resolver) {

        // add EC-/PEM-Parser
        resolver.addParser(ECKey.class, (encoded) -> {
            try {
                return (ECKey) ECKey.parseFromPEMEncodedObjects(encoded);
            } catch (JOSEException e) {
                throw new EdcException(e);
            }
        });
        resolver.addParser(PrivateKeyWrapper.class, (encoded) -> {
            try {
                var ecKey = (ECKey) ECKey.parseFromPEMEncodedObjects(encoded);
                return new EcPrivateKeyWrapper(ecKey);
            } catch (JOSEException e) {
                throw new EdcException(e);
            }
        });

    }
}
