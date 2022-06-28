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

package org.eclipse.dataspaceconnector.core.security.hashicorpvault;

import org.bouncycastle.operator.OperatorCreationException;
import org.eclipse.dataspaceconnector.common.testfixtures.X509CertificateTestUtil;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

class HashicorpCertificateResolverTest {
    private static final String key = "key";

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
        var certificateExpected = X509CertificateTestUtil.generateCertificate(5, "Test");
        var pem = X509CertificateTestUtil.convertToPem(certificateExpected);
        Mockito.when(vault.resolveSecret(key)).thenReturn(pem);

        // invoke
        certificateResolver.resolveCertificate(key);

        // verify
        Mockito.verify(vault, Mockito.times(1)).resolveSecret(key);
    }
}
