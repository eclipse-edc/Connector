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

package org.eclipse.edc.iam.did.crypto.key;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import org.eclipse.edc.iam.did.crypto.CryptoException;
import org.eclipse.edc.iam.did.spi.key.PrivateKeyWrapper;

public class RsaPrivateKeyWrapper implements PrivateKeyWrapper {
    private final RSAKey privateKey;

    public RsaPrivateKeyWrapper(RSAKey privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public JWEDecrypter decrypter() {
        try {
            return new RSADecrypter(privateKey);
        } catch (JOSEException e) {
            throw new CryptoException(e);
        }
    }

    @Override
    public JWSSigner signer() {
        try {
            return new RSASSASigner(privateKey);
        } catch (JOSEException e) {
            throw new CryptoException(e);
        }
    }
}
