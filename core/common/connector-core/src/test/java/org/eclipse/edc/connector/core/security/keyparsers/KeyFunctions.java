/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.core.security.keyparsers;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;

import java.util.Date;
import java.util.UUID;

class KeyFunctions {
    public static RSAKey createRsa(int len) {
        try {
            return new RSAKeyGenerator(len)
                    .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key (optional)
                    .keyID(UUID.randomUUID().toString()) // give the key a unique ID (optional)
                    .issueTime(new Date()) // issued-at timestamp (optional)
                    .generate();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public static JWK createOkp(Curve curve) {
        try {
            return new OctetKeyPairGenerator(curve)
                    .keyID(UUID.randomUUID().toString())
                    .generate();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }

    }

    public static ECKey createEc(Curve curve) {
        try {
            return new ECKeyGenerator(curve)
                    .keyID("test-kid")
                    .keyUse(KeyUse.SIGNATURE)
                    .provider(BouncyCastleProviderSingleton.getInstance()) // required for secp256k1, no support in Nimbus or Java 17
                    .generate();

        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

}
