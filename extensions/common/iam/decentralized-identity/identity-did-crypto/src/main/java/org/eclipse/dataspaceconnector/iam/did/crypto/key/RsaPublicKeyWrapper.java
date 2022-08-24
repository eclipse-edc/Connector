/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.iam.did.crypto.key;

import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;

import java.security.interfaces.RSAPublicKey;

public class RsaPublicKeyWrapper implements PublicKeyWrapper {
    private final RSAPublicKey publicKey;

    public RsaPublicKeyWrapper(RSAPublicKey publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public JWEEncrypter encrypter() {
        return new RSAEncrypter(publicKey);
    }

    @Override
    public JWSVerifier verifier() {
        return new RSASSAVerifier(publicKey);
    }

    @Override
    public JWEAlgorithm jweAlgorithm() {
        return JWEAlgorithm.RSA_OAEP_256;
    }
}
