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

import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;

import java.security.interfaces.RSAPrivateKey;

public class RsaPrivateKeyWrapper implements PrivateKeyWrapper {
    private final RSAPrivateKey privateKey;

    public RsaPrivateKeyWrapper(RSAPrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public JWEDecrypter decrypter() {
        return new RSADecrypter(privateKey);
    }

    @Override
    public JWSSigner signer() {
        return new RSASSASigner(privateKey);
    }
}
