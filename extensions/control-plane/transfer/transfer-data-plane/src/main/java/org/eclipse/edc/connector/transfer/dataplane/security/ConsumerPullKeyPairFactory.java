/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.connector.transfer.dataplane.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.NotNull;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;
import java.util.UUID;

public class ConsumerPullKeyPairFactory {

    private final PrivateKeyResolver privateKeyResolver;
    private final Vault vault;

    public ConsumerPullKeyPairFactory(PrivateKeyResolver privateKeyResolver, Vault vault) {
        this.privateKeyResolver = privateKeyResolver;
        this.vault = vault;
    }

    public Result<KeyPair> fromConfig(@NotNull String publicKeyAlias, @NotNull String privateKeyAlias) {
        return publicKey(publicKeyAlias)
                .compose(publicKey -> privateKey(privateKeyAlias)
                        .map(privateKey -> new KeyPair(publicKey, privateKey)));
    }

    public KeyPair defaultKeyPair() {
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

    @NotNull
    private Result<PublicKey> publicKey(String alias) {
        return Optional.ofNullable(vault.resolveSecret(alias))
                .map(ConsumerPullKeyPairFactory::convertPemToPublicKey)
                .orElse(Result.failure("Failed to resolve public key with alias: " + alias));
    }

    @NotNull
    private Result<PrivateKey> privateKey(String alias) {
        return Optional.ofNullable(privateKeyResolver.resolvePrivateKey(alias, PrivateKey.class))
                .map(Result::success)
                .orElse(Result.failure("Failed to resolve private key with alias: " + alias));
    }

    @NotNull
    private static Result<PublicKey> convertPemToPublicKey(String pem) {
        try {
            var jwk = JWK.parseFromPEMEncodedObjects(pem);
            if (jwk instanceof RSAKey) {
                return Result.success(jwk.toRSAKey().toPublicKey());
            } else if (jwk instanceof ECKey) {
                return Result.success(jwk.toECKey().toPublicKey());
            } else {
                return Result.failure(String.format("Public key algorithm %s is not supported", jwk.getAlgorithm().toString()));
            }
        } catch (JOSEException e) {
            return Result.failure("Failed to parse private key: " + e.getMessage());
        }
    }
}
