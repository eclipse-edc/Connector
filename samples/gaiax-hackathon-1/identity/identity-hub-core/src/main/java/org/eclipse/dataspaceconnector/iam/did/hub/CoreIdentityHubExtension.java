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
package org.eclipse.dataspaceconnector.iam.did.hub;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.IdentityHub;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.IdentityHubStore;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.util.Set;
import java.util.function.Supplier;

import static java.lang.String.format;

/**
 *
 */
public class CoreIdentityHubExtension implements ServiceExtension {
    @EdcSetting
    private static final String PRIVATE_KEY_ALIAS = "dataspaceconnector.did.private.key.alias";

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

        var hub = new IdentityHubImpl(hubStore, resolverPair.privateKeyResolver, resolverPair.publicKeyResolver, objectMapper);
        context.registerService(IdentityHub.class, hub);

        var controller = new IdentityHubController(hub);
        var webService = context.getService(WebService.class);
        webService.registerController(controller);

        context.getMonitor().info("Initialized Core Identity Hub extension");
    }

    // TODO: TEMPORARY local key management to be replaced by vault storage
    private static final String TEST_KEYSTORE = "edc-test-keystore.jks";
    private static final String PASSWORD = "test123";

    private ResolverPair temporaryLoadResolvers(ServiceExtensionContext context) {
        var keys = loadKeys(context.getMonitor());
        if (keys == null) {
            return new ResolverPair(() -> null, (dis) -> null);
        }
        var privateKeyAlias = context.getSetting(PRIVATE_KEY_ALIAS, "privateKeyAlias");
        // var privateKeyResolver = context.getService(PrivateKeyResolver.class);
        PrivateKeyResolver delegateResolver = id -> {
            try {
                return keys.toRSAPrivateKey();
            } catch (JOSEException e) {
                throw new EdcException(e);
            }
        };

        Supplier<RSAPrivateKey> privateKeyResolver = () -> delegateResolver.resolvePrivateKey(privateKeyAlias);

        DidPublicKeyResolver publicKeyResolver = (did) -> {
            try {
                return keys.toPublicKey();
            } catch (JOSEException e) {
                throw new EdcException(e);
            }
        };
        return new ResolverPair(privateKeyResolver, publicKeyResolver);
    }

    @Nullable
    private RSAKey loadKeys(Monitor monitor) {
        try {
            var url = Paths.get("secrets" + File.separator + TEST_KEYSTORE).toUri().toURL();
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream stream = url.openStream()) {
                keyStore.load(stream, PASSWORD.toCharArray());
            }
            return RSAKey.load(keyStore, "testkey", PASSWORD.toCharArray());
        } catch (Exception e) {
            monitor.info(format("Cannot load test keys - the keystore %s should be placed in the secrets directory", TEST_KEYSTORE));
            return null;
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
