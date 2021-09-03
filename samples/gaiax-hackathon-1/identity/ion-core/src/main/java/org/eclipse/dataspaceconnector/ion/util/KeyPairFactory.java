/*
 *  Copyright (c) 2020, 2020-2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.ion.util;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import org.eclipse.dataspaceconnector.ion.IonException;
import org.eclipse.dataspaceconnector.ion.model.JwkKeyPair;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;

/**
 * Convenience class that generates an Elliptic Curve Keypair according to the SECP256K1 spec.
 */
public class KeyPairFactory {

    public static JwkKeyPair generateKeyPair() {
        try {

            ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            g.initialize(ecSpec, new SecureRandom());
            KeyPair keyPair = g.generateKeyPair();


            JWK jwk = new ECKey.Builder(Curve.SECP256K1, (ECPublicKey) keyPair.getPublic())
                    .privateKey((ECPrivateKey) keyPair.getPrivate())
                    .build();


            return JwkKeyPair.from(jwk);
        } catch (Exception e) {
            throw new IonException(e);
        }
    }

}
