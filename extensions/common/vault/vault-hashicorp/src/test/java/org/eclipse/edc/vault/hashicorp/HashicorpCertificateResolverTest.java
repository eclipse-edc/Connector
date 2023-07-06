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

import org.bouncycastle.operator.OperatorCreationException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.vault.hashicorp.util.X509CertificateTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

class HashicorpCertificateResolverTest {
    private static final String KEY = "key";

    // mocks
    private HashicorpCertificateResolver certificateResolver;
    private HashicorpVault vault;

    @BeforeEach
    void setup() {
        vault = Mockito.mock(HashicorpVault.class);
        final Monitor monitor = Mockito.mock(Monitor.class);
        certificateResolver = new HashicorpCertificateResolver(vault, monitor);
    }

    @Test
    void resolveCertificate() throws CertificateException, IOException, NoSuchAlgorithmException, OperatorCreationException {
        // prepare
        X509Certificate certificateExpected = X509CertificateTestUtil.generateCertificate(5, "Test");
        String pem = X509CertificateTestUtil.convertToPem(certificateExpected);
        Mockito.when(vault.resolveSecret(KEY)).thenReturn(pem);

        // invoke
        certificateResolver.resolveCertificate(KEY);

        // verify
        Mockito.verify(vault, Mockito.times(1)).resolveSecret(KEY);
    }

    @Test
    void nullIfVaultEmpty() {
        // prepare
        Mockito.when(vault.resolveSecret(KEY)).thenReturn(null);

        // invoke
        final X509Certificate certificate = certificateResolver.resolveCertificate(KEY);

        // verify
        Assertions.assertNull(certificate);
    }
}
