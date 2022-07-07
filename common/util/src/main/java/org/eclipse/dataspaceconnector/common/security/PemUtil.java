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

package org.eclipse.dataspaceconnector.common.security;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public final class PemUtil {
    private static final String HEADER = "-----BEGIN CERTIFICATE-----";
    private static final String FOOTER = "-----END CERTIFICATE-----";

    public static X509Certificate readX509Certificate(@NotNull String encoded) {
        encoded = encoded.replace(HEADER, "").replaceAll(System.lineSeparator(), "").replace(FOOTER, "");

        try {
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            return (X509Certificate) fact.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(encoded.getBytes())));
        } catch (GeneralSecurityException e) {
            throw new EdcException(e.getMessage(), e);
        }
    }

    public static String convertCertificateToPem(@NotNull X509Certificate certificate) {
        var base64Encoder = Base64.getMimeEncoder(64, System.lineSeparator().getBytes(StandardCharsets.UTF_8));
        try {
            var encodedCert = new String(base64Encoder.encode(certificate.getEncoded()));
            return String.format("%s%s%s%s%s",
                    HEADER,
                    System.lineSeparator(),
                    encodedCert,
                    System.lineSeparator(),
                    FOOTER);
        } catch (CertificateEncodingException e) {
            throw new EdcException(e.getMessage(), e);
        }
    }
}
