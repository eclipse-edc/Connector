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

import org.eclipse.dataspaceconnector.common.testfixtures.X509CertificateTestUtil;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;


class HashicorpCertificateResolverTest {
    private static final String KEY = "key";

    private HashicorpCertificateResolver certificateResolver;
    private HashicorpVault vault;

    @BeforeEach
    void setup() {
        vault = Mockito.mock(HashicorpVault.class);
        final Monitor monitor = Mockito.mock(Monitor.class);
        certificateResolver = new HashicorpCertificateResolver(vault, monitor);
    }

    @Test
    void resolveCertificate() throws RuntimeException, IOException {
        var certificateExpected = X509CertificateTestUtil.generateCertificate(5, "Test");
        var pem = X509CertificateTestUtil.convertToPem(certificateExpected);
        Mockito.when(vault.resolveSecret(KEY)).thenReturn(pem);

        certificateResolver.resolveCertificate(KEY);

        Mockito.verify(vault, Mockito.times(1)).resolveSecret(KEY);
    }
}
