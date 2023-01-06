/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.iam.oauth2.jwt;

import org.eclipse.edc.jwt.spi.JwtDecorator;
import org.eclipse.edc.spi.EdcException;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;

import static org.eclipse.edc.iam.oauth2.jwt.Fingerprint.sha1Base64Fingerprint;

/**
 * Creates the 'x5t' header containing the base64url-encoded SHA-1 thumbprint of the DER encoding of the thumbprint of the
 * X.509 certificate corresponding to the key used to sign the JWT. This header is requested by some Oauth2 servers.
 */
public class X509CertificateDecorator implements JwtDecorator {
    private final byte[] certificate;

    public X509CertificateDecorator(X509Certificate certificate) {
        try {
            this.certificate = certificate.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public Map<String, Object> headers() {
        return Map.of("x5t", sha1Base64Fingerprint(certificate));
    }

    @Override
    public Map<String, Object> claims() {
        return Collections.emptyMap();
    }
}
