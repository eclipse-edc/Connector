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

package org.eclipse.edc.connector.dataplane.transfer;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.connector.dataplane.transfer.security.KeyPairWrapperImpl;
import org.eclipse.edc.connector.dataplane.transfer.security.NoopDataEncrypter;
import org.eclipse.edc.connector.dataplane.transfer.security.PublicKeyParser;
import org.eclipse.edc.connector.dataplane.transfer.spi.security.DataEncrypter;
import org.eclipse.edc.connector.dataplane.transfer.spi.security.KeyPairWrapper;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
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

import static org.eclipse.edc.connector.dataplane.transfer.DataPlaneTransferConfig.TOKEN_SIGNER_PRIVATE_KEY_ALIAS;
import static org.eclipse.edc.connector.dataplane.transfer.DataPlaneTransferConfig.TOKEN_VERIFIER_PUBLIC_KEY_ALIAS;

/**
 * Provides default service implementations for fallback
 * Omitted {@link Extension since this module already contains {@link DataPlaneTransferExtension }}
 */
public class DataPlaneTransferDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Transfer Default Services";

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public DataEncrypter getDataEncrypter(ServiceExtensionContext context) {
        context.getMonitor().warning("No DataEncrypter registered, a no-op implementation will be used, not suitable for production environments");
        return new NoopDataEncrypter();
    }

    @Provider
    public KeyPairWrapper getKeyPairWrapper(ServiceExtensionContext context) {
        var pubKeyAlias = context.getSetting(TOKEN_VERIFIER_PUBLIC_KEY_ALIAS, null);
        var privKeyAlias = context.getSetting(TOKEN_SIGNER_PRIVATE_KEY_ALIAS, null);
        if (pubKeyAlias == null && privKeyAlias == null) {
            context.getMonitor().debug(() -> "Either private (n)or public key alias not provided, a random key pair will be generated");
            return new KeyPairWrapperImpl(randomKeyPair());
        }
        Objects.requireNonNull(pubKeyAlias, "public key alias");
        Objects.requireNonNull(privKeyAlias, "private key alias");
        var pubKey = getPublicKey(context, pubKeyAlias);
        var privKey = getPrivateKey(context, privKeyAlias);
        return new KeyPairWrapperImpl(new KeyPair(pubKey, privKey));
    }

    private static @NotNull PublicKey getPublicKey(ServiceExtensionContext context, String alias) {
        var vault = context.getService(Vault.class);
        var publicKeyPem = vault.resolveSecret(alias);
        Objects.requireNonNull(publicKeyPem, "Failed to resolve public key with alias: " + alias + " from vault");
        return PublicKeyParser.from(publicKeyPem);
    }

    private static @NotNull PrivateKey getPrivateKey(ServiceExtensionContext context, String alias) {
        var resolver = context.getService(PrivateKeyResolver.class);
        var privateKey = resolver.resolvePrivateKey(alias, PrivateKey.class);
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
