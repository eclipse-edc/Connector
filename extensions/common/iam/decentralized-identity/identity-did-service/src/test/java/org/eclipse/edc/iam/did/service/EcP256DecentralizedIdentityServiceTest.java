/*
 *  Copyright (c) 2021 - 2022 Microsoft Corporation
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

package org.eclipse.edc.iam.did.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import org.eclipse.edc.iam.did.crypto.key.KeyPairFactory;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;

public class EcP256DecentralizedIdentityServiceTest extends BaseDecentralizedIdentityServiceTest {

    public EcP256DecentralizedIdentityServiceTest() throws JOSEException {
        super(KeyPairFactory.generateKeyPairP256().toKeyPair());
    }

    @Override
    protected @NotNull JWK generateKeyPair() {
        return KeyPairFactory.generateKeyPairP256();
    }

    @Override
    protected JWK toJwk(PublicKey publicKey) {
        var ec = (ECPublicKey) publicKey;
        return new ECKey.Builder(Curve.P_256, ec).build();
    }

}
