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
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.did.hub.IdentityHubClientImpl;
import org.eclipse.dataspaceconnector.iam.did.hub.IdentityHubController;
import org.eclipse.dataspaceconnector.iam.did.hub.IdentityHubImpl;
import org.eclipse.dataspaceconnector.iam.did.resolver.DidPublicKeyResolverImpl;
import org.eclipse.dataspaceconnector.iam.did.resolver.DidResolverImpl;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHub;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubClient;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubStore;
import org.eclipse.dataspaceconnector.iam.did.spi.resolver.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolver.DidResolver;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Set;
import java.util.function.Supplier;

import static org.eclipse.dataspaceconnector.iam.did.hub.TemporaryKeyLoader.loadKeys;

/**
 *
 */
public class IdentityDidCoreHubExtension implements ServiceExtension {
    private static final String RESOLVER_URL = "http://23.97.144.59:3000/identifiers/";

    @EdcSetting
    private static final String PRIVATE_KEY_ALIAS = "dataspaceconnector.did.private.key.alias";

    @Override
    public Set<String> provides() {
        return Set.of("identity-did-core");
    }

    @Override
    public Set<String> requires() {
        return Set.of("identity-hub-store");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var hubStore = context.getService(IdentityHubStore.class);

        var objectMapper = context.getTypeManager().getMapper();

        // TODO: implement key resolvers
        var resolverPair = temporaryLoadResolvers(context);
        context.registerService(DidPublicKeyResolver.class, resolverPair.publicKeyResolver);

        var hub = new IdentityHubImpl(hubStore, resolverPair.privateKeyResolver, resolverPair.publicKeyResolver, objectMapper);
        context.registerService(IdentityHub.class, hub);

        var controller = new IdentityHubController(hub);
        var webService = context.getService(WebService.class);
        webService.registerController(controller);

        var httpClient = context.getService(OkHttpClient.class);
        var typeManager = context.getService(TypeManager.class);
        var didResolver = new DidResolverImpl(RESOLVER_URL, httpClient, typeManager.getMapper());
        context.registerService(DidResolver.class, didResolver);

        var hubClient = new IdentityHubClientImpl(resolverPair.privateKeyResolver, httpClient, objectMapper);
        context.registerService(IdentityHubClient.class, hubClient);

        context.getMonitor().info("Initialized Identity Did Core extension");
    }

    // TODO: TEMPORARY local key management to be replaced by vault storage
    private ResolverPair temporaryLoadResolvers(ServiceExtensionContext context) {
        try {
            var keys = loadKeys(context.getMonitor());
            if (keys == null) {
                return new ResolverPair(() -> null, (dis) -> null);
            }
            PublicKey publicKey = keys.toPublicKey();
            RSAPrivateKey privateKey = keys.toRSAPrivateKey();

            var privateKeyAlias = context.getSetting(PRIVATE_KEY_ALIAS, "privateKeyAlias");
            // var privateKeyResolver = context.getService(PrivateKeyResolver.class);
            PrivateKeyResolver delegateResolver = id -> privateKey;

            Supplier<RSAPrivateKey> privateKeyResolver = () -> delegateResolver.resolvePrivateKey(privateKeyAlias);

            DidPublicKeyResolver publicKeyResolver = new DidPublicKeyResolverImpl(publicKey);
            return new ResolverPair(privateKeyResolver, publicKeyResolver);
        } catch (JOSEException e) {
            throw new EdcException(e);
        }
    }


    private static class ResolverPair {
        Supplier<RSAPrivateKey> privateKeyResolver;
        DidPublicKeyResolver publicKeyResolver;

        public ResolverPair(Supplier<RSAPrivateKey> privateKeyResolver, DidPublicKeyResolver publicKeyResolver) {
            this.privateKeyResolver = privateKeyResolver;
            this.publicKeyResolver = publicKeyResolver;
        }
    }
}
