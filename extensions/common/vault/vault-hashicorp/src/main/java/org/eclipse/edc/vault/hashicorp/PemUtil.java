/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.Provider;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

final class PemUtil {

    private static final Provider PROVIDER = new BouncyCastleProvider();
    private static final JcaX509CertificateConverter X509_CONVERTER =
            new JcaX509CertificateConverter().setProvider(PROVIDER);

    private PemUtil() {
        throw new IllegalStateException("Private constructor invocation disallowed");
    }

    public static X509Certificate readX509Certificate(@NotNull InputStream inputStream) {
        try {
            X509CertificateHolder x509CertificateHolder = parsePem(inputStream);
            if (x509CertificateHolder == null) {
                return null;
            }
            return X509_CONVERTER.getCertificate(x509CertificateHolder);
        } catch (IOException | CertificateException e) {
            throw new RuntimeException(e);
        }

    }

    @SuppressWarnings("unchecked")
    private static <T> T parsePem(@NotNull InputStream inputStream) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream)) {
            PEMParser pemParser = new PEMParser(reader);
            return (T) pemParser.readObject();
        }
    }
}
