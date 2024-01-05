/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.verifiablecredentials;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDHEncrypter;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyConverter;
import org.eclipse.edc.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.edc.spi.EdcException;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;

public class TestFunctions {

    @NotNull
    public static PublicKeyWrapper createPublicKeyWrapper(ECKey vpSigningKey) {
        return new PublicKeyWrapper() {
            @Override
            public JWEEncrypter encrypter() {
                try {
                    return new ECDHEncrypter(vpSigningKey);
                } catch (JOSEException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public JWSVerifier verifier() {
                try {
                    return new ECDSAVerifier(vpSigningKey);
                } catch (JOSEException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static PublicKey createPublicKey(ECKey signingKey) {
        return KeyConverter.toJavaKeys(List.of(signingKey))
                .stream()
                .filter(k -> k instanceof PublicKey)
                .map(k -> (PublicKey) k)
                .findFirst()
                .orElseThrow(() -> new EdcException("EC Key cannot be converted to a Java PublicKey"));

    }
}
