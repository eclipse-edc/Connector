/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.iam.oauth2.impl;

import com.auth0.jwt.interfaces.RSAKeyProvider;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static java.lang.String.format;

/**
 * Shim from a {@link PrivateKeyResolver} and {@link CertificateResolver} to a {@link RSAKeyProvider} required by the JWT signer.
 */
public class PairedProviderWrapper implements RSAKeyProvider {
    private final PrivateKeyResolver privateKeyResolver;
    private final CertificateResolver certificateResolver;
    private final String privateKeyId;

    public PairedProviderWrapper(PrivateKeyResolver privateKeyResolver, CertificateResolver certificateResolver, String privateKeyId) {
        this.privateKeyResolver = privateKeyResolver;
        this.certificateResolver = certificateResolver;
        this.privateKeyId = privateKeyId;
    }

    @Override
    public RSAPublicKey getPublicKeyById(String keyId) {
        X509Certificate certificate = certificateResolver.resolveCertificate(keyId);
        if (certificate == null) {
            return null;
        }
        PublicKey publicKey = certificate.getPublicKey();
        if (publicKey instanceof RSAPublicKey) {
            return (RSAPublicKey) publicKey;
        }
        throw new EdcException(format("Unsupported certificate type for id %s: %s", keyId, publicKey.getClass().getName()));
    }

    @Override
    public RSAPrivateKey getPrivateKey() {
        return privateKeyResolver != null ? privateKeyResolver.resolvePrivateKey(privateKeyId, RSAPrivateKey.class) : null;
    }

    @Override
    public String getPrivateKeyId() {
        return privateKeyId;
    }
}

