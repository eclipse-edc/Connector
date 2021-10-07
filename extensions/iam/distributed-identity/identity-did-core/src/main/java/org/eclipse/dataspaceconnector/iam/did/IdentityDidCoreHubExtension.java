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
import org.eclipse.dataspaceconnector.iam.did.hub.IdentityHubClientImpl;
import org.eclipse.dataspaceconnector.iam.did.hub.IdentityHubController;
import org.eclipse.dataspaceconnector.iam.did.hub.IdentityHubImpl;
import org.eclipse.dataspaceconnector.iam.did.resolver.DefaultDidPublicKeyResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.CryptoException;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHub;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubClient;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubStore;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.keys.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.ion.crypto.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;
import java.util.function.Supplier;


public class IdentityDidCoreHubExtension implements ServiceExtension {

    @EdcSetting
    private static final String PRIVATE_KEY_ALIAS = "edc.did.private.key.alias";

    @Override
    public Set<String> provides() {
        return Set.of(IdentityHub.FEATURE, IdentityHubClient.FEATURE, DidPublicKeyResolver.FEATURE);
    }

    @Override
    public Set<String> requires() {
        return Set.of(IdentityHubStore.FEATURE, PrivateKeyResolver.FEATURE, DidResolver.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var hubStore = context.getService(IdentityHubStore.class);

        var objectMapper = context.getTypeManager().getMapper();

        var publicKeyResolver = context.getService(DidPublicKeyResolver.class, true);
        if (publicKeyResolver == null) {
            //registering ION Public Key Resolver
            var resolver = context.getService(DidResolver.class);
            publicKeyResolver = new DefaultDidPublicKeyResolver(resolver);
            context.registerService(DidPublicKeyResolver.class, publicKeyResolver);
        }
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
                throw new CryptoException(e);
            }
        });
        resolver.addParser(PrivateKeyWrapper.class, (encoded) -> {
            try {
                var ecKey = (ECKey) ECKey.parseFromPEMEncodedObjects(encoded);
                return new EcPrivateKeyWrapper(ecKey);
            } catch (JOSEException e) {
                throw new CryptoException(e);
            }
        });

    }
}
