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
 *       Mercedes-Benz Tech Innovation GmbH - Initial Test
 *
 */

package org.eclipse.edc.keys;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class VaultCertificateResolverTest {
    private static final String KEY = "key";
    private static final String TEST_CERT_FILE = "testCert.pem";

    private VaultCertificateResolver certificateResolver;
    private Vault vault;

    @BeforeEach
    void setup() {
        vault = mock(Vault.class);
        certificateResolver = new VaultCertificateResolver(vault);
    }

    @Test
    void resolveCertificate() throws RuntimeException, IOException {
        var classloader = Thread.currentThread().getContextClassLoader();
        var pemExpected =  new String(Objects.requireNonNull(classloader.getResourceAsStream(TEST_CERT_FILE)).readAllBytes());
        when(vault.resolveSecret(KEY)).thenReturn(pemExpected);

        var certificate = Objects.requireNonNull(certificateResolver.resolveCertificate(KEY));
        var pemReceived = convertCertificateToPem(certificate);

        verify(vault, times(1)).resolveSecret(KEY);
        assertThat(pemReceived.split("\\R")).isEqualTo(pemExpected.split("\\R"));
    }

    @Test
    void resolveCertificate_notFound() {
        when(vault.resolveSecret(KEY)).thenReturn(null);

        var certificate = certificateResolver.resolveCertificate(KEY);

        verify(vault, times(1)).resolveSecret(KEY);
        assertThat(certificate).isNull();
    }

    @Test
    void resolveCertificate_conversionError() {
        when(vault.resolveSecret(KEY)).thenReturn("Not a PEM");

        Exception exception = assertThrows(EdcException.class, () -> certificateResolver.resolveCertificate(KEY));

        assertThat(exception.getMessage()).isEqualTo(String.format(VaultCertificateResolver.EDC_EXCEPTION_MESSAGE, KEY));
    }

    private String convertCertificateToPem(@NotNull X509Certificate certificate) {
        var base64Encoder = Base64.getMimeEncoder(64, System.lineSeparator().getBytes(StandardCharsets.UTF_8));
        try {
            var encodedCert = new String(base64Encoder.encode(certificate.getEncoded()));
            return String.format("%s%s%s%s%s",
                    VaultCertificateResolver.HEADER,
                    System.lineSeparator(),
                    encodedCert,
                    System.lineSeparator(),
                    VaultCertificateResolver.FOOTER);
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
