/*
 *  Copyright (c) 2020 - 2021 Microsoft Corporation
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

package org.eclipse.edc.iam.did.crypto.key;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.eclipse.edc.iam.did.crypto.CryptoException;

import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;
import java.util.UUID;

/**
 * Convenience class that generates an Elliptic Curve Keypair according to the SECP256K1 spec.
 *
 * @deprecated do not use anymore
 */
@Deprecated(forRemoval = true)
public class KeyPairFactory {

    private KeyPairFactory() {
    }

    /**
     * Generates an Elliptic Curve Public/Private Key pair on the P-256 curve
     *
     * @return A newly generated {@link ECKey}
     */
    public static ECKey generateKeyPairP256() {
        try {
            var ecSpec = new ECGenParameterSpec("secp256r1");
            var g = KeyPairGenerator.getInstance("EC");
            g.initialize(ecSpec, new SecureRandom());
            var keyPair = g.generateKeyPair();
            return new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
                    .privateKey((ECPrivateKey) keyPair.getPrivate())
                    .build();
        } catch (Exception e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Generates an RSA Public/Private Key pair
     *
     * @return A newly generated {@link com.nimbusds.jose.jwk.RSAKey}
     */
    public static RSAKey generateKeyPairRsa() {
        try {
            return new RSAKeyGenerator(4096)
                    .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key (optional)
                    .keyID(UUID.randomUUID().toString()) // give the key a unique ID (optional)
                    .issueTime(new Date()) // issued-at timestamp (optional)
                    .generate();
        } catch (JOSEException e) {
            throw new CryptoException(e);
        }
    }
}
