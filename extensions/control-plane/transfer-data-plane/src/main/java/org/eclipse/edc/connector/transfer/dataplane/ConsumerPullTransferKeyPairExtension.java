/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.dataplane;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.connector.transfer.dataplane.security.ConsumerPullTransferKeyPair;
import org.eclipse.edc.connector.transfer.dataplane.security.PublicKeyParser;
import org.eclipse.edc.connector.transfer.dataplane.spi.security.KeyPairWrapper;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.TOKEN_SIGNER_PRIVATE_KEY_ALIAS;
import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.TOKEN_VERIFIER_PUBLIC_KEY_ALIAS;

@Extension(value = ConsumerPullTransferProxyResolverExtension.NAME)
public class ConsumerPullTransferKeyPairExtension implements ServiceExtension {

    public static final String NAME = "Consumer Pull Transfer Key Pair";

    @Inject(required = false)
    private PrivateKeyResolver privateKeyResolver;

    @Inject(required = false)
    private Vault vault;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public KeyPairWrapper keyPairWrapper(ServiceExtensionContext context) {
        var pubKeyAlias = context.getSetting(TOKEN_VERIFIER_PUBLIC_KEY_ALIAS, null);
        var privKeyAlias = context.getSetting(TOKEN_SIGNER_PRIVATE_KEY_ALIAS, null);
        if (pubKeyAlias == null && privKeyAlias == null) {
            context.getMonitor().info(() -> "Either private (n)or public key alias not provided for 'consumer pull' transfer, a random key pair will be generated");
            return new ConsumerPullTransferKeyPair(randomKeyPair());
        }
        Objects.requireNonNull(privateKeyResolver, "private key resolver");
        Objects.requireNonNull(vault, "vault");
        Objects.requireNonNull(pubKeyAlias, "public key alias");
        Objects.requireNonNull(privKeyAlias, "private key alias");
        var publicKey = getPublicKey(pubKeyAlias);
        var privateKey = getPrivateKey(privKeyAlias);
        return new ConsumerPullTransferKeyPair(new KeyPair(publicKey, privateKey));
    }

    @NotNull
    private PublicKey getPublicKey(String alias) {
        var publicKeyPem = vault.resolveSecret(alias);
        Objects.requireNonNull(publicKeyPem, "Failed to resolve public key with alias: " + alias + " from vault");
        return PublicKeyParser.from(publicKeyPem);
    }

    @NotNull
    private PrivateKey getPrivateKey(String alias) {
        var privateKey = privateKeyResolver.resolvePrivateKey(alias, PrivateKey.class);
        Objects.requireNonNull(privateKey, "Failed to resolve private key with alias: " + alias);
        return privateKey;
    }

    private static KeyPair randomKeyPair() {
        try {
            return new ECKeyGenerator(Curve.P_256)
                    .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                    .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                    .generate()
                    .toKeyPair();
        } catch (JOSEException e) {
            throw new EdcException(e);
        }
    }
}
