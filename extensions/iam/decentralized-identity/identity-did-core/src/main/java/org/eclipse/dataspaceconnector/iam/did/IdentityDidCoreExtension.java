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
import org.eclipse.dataspaceconnector.iam.did.resolution.DidPublicKeyResolverImpl;
import org.eclipse.dataspaceconnector.iam.did.resolution.DidResolverRegistryImpl;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHub;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubClient;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubStore;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckResult;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;

import java.util.function.Supplier;


@Provides({IdentityHub.class, IdentityHubClient.class, DidResolverRegistry.class, DidPublicKeyResolver.class})
public class IdentityDidCoreExtension implements ServiceExtension {

    @Inject
    private IdentityHubStore hubStore;
    @Inject
    private WebService webService;

    public IdentityDidCoreExtension() {
    }

    public IdentityDidCoreExtension(IdentityHubStore hubStore, WebService webService) {
        this.hubStore = hubStore;
        this.webService = webService;
    }

    @Override
    public String name() {
        return "Identity Did Core";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        var objectMapper = context.getTypeManager().getMapper();

        var resolverRegistry = new DidResolverRegistryImpl();
        context.registerService(DidResolverRegistry.class, resolverRegistry);

        var publicKeyResolver = new DidPublicKeyResolverImpl(resolverRegistry);
        context.registerService(DidPublicKeyResolver.class, publicKeyResolver);

        var privateKeyResolver = context.getService(PrivateKeyResolver.class);
        registerParsers(privateKeyResolver);

        PrivateKeyWrapper privateKeyWrapper = privateKeyResolver.resolvePrivateKey(context.getConnectorId(), PrivateKeyWrapper.class);
        Supplier<PrivateKeyWrapper> supplier = () -> privateKeyWrapper;
        var hub = new IdentityHubImpl(hubStore, supplier, publicKeyResolver, objectMapper);
        context.registerService(IdentityHub.class, hub);

        var controller = new IdentityHubController(hub);
        webService.registerResource(controller);

        // contribute to the liveness probe
        var hcs = context.getService(HealthCheckService.class, true);
        if (hcs != null) {
            hcs.addReadinessProvider(() -> HealthCheckResult.Builder.newInstance().component("IdentityHub Controller").build());
        }

        var httpClient = context.getService(OkHttpClient.class);

        var hubClient = new IdentityHubClientImpl(supplier, httpClient, objectMapper);
        context.registerService(IdentityHubClient.class, hubClient);
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
