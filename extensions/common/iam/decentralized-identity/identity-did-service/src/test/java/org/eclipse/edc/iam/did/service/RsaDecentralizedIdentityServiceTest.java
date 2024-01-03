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
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.iam.did.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import org.eclipse.edc.iam.did.crypto.key.KeyPairFactory;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

public class RsaDecentralizedIdentityServiceTest extends BaseDecentralizedIdentityServiceTest {

    public RsaDecentralizedIdentityServiceTest() throws JOSEException {
        super(KeyPairFactory.generateKeyPairRsa().toKeyPair());
    }

    @Override
    protected @NotNull JWK generateKeyPair() {
        return KeyPairFactory.generateKeyPairRsa();
    }

    @Override
    protected JWK toJwk(PublicKey publicKey) {
        return new RSAKey.Builder((RSAPublicKey) publicKey).build();
    }

}
