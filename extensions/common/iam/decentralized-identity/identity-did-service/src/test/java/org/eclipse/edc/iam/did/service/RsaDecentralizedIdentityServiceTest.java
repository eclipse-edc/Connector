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

import com.nimbusds.jose.jwk.JWK;
import org.eclipse.edc.iam.did.crypto.key.KeyPairFactory;
import org.eclipse.edc.iam.did.crypto.key.RsaPrivateKeyWrapper;
import org.eclipse.edc.iam.did.spi.key.PrivateKeyWrapper;
import org.jetbrains.annotations.NotNull;

public class RsaDecentralizedIdentityServiceTest extends BaseDecentralizedIdentityServiceTest {

    public RsaDecentralizedIdentityServiceTest() {
        super(KeyPairFactory.generateKeyPairRsa());
    }

    @Override
    protected @NotNull JWK generateKeyPair() {
        return KeyPairFactory.generateKeyPairRsa();
    }

    @Override
    protected @NotNull PrivateKeyWrapper privateKeyWrapper(JWK keyPair) {
        return new RsaPrivateKeyWrapper(keyPair.toRSAKey());
    }
}
