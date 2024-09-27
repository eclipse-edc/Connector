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

import org.eclipse.edc.spi.iam.TokenParameters;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class X509CertificateDecoratorTest {

    private static final String TEST_CERT_FILE = "cert.pem";
    private static final String HEADER = "-----BEGIN CERTIFICATE-----";
    private static final String FOOTER = "-----END CERTIFICATE-----";

    private static X509Certificate createCertificate() throws CertificateException, IOException {
        var classloader = Thread.currentThread().getContextClassLoader();
        var pem = new String(Objects.requireNonNull(classloader.getResourceAsStream(TEST_CERT_FILE)).readAllBytes());
        var encoded = pem.replace(HEADER, "").replaceAll("\\R", "").replace(FOOTER, "");
        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        return (X509Certificate) fact.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(encoded.getBytes())));
    }

    @Test
    void verifyDecorator() throws CertificateException, IOException {
        var certificate = createCertificate();
        var decorator = new X509CertificateDecorator(certificate);

        var builder = TokenParameters.Builder.newInstance();
        decorator.decorate(builder);

        var tokenParams = builder.build();
        assertThat(tokenParams.getClaims()).isEmpty();
        assertThat(tokenParams.getHeaders()).containsOnlyKeys("x5t");
    }
}