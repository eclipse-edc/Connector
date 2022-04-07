/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.did.crypto.key;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDHEncrypter;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.dataspaceconnector.iam.did.crypto.CryptoException;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;

public class EcPublicKeyWrapper implements PublicKeyWrapper {
    private final ECKey publicKey;

    public EcPublicKeyWrapper(ECKey publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public JWEEncrypter encrypter() {
        try {
            return new ECDHEncrypter(publicKey);
        } catch (JOSEException e) {
            throw new CryptoException(e);
        }
    }

    @Override
    public JWSVerifier verifier() {
        try {
            return new ECDSAVerifier(publicKey);
        } catch (JOSEException e) {
            throw new CryptoException(e);
        }
    }
}
