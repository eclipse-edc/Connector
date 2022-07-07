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

import org.eclipse.dataspaceconnector.common.security.PemUtil;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class HashicorpCertificateResolverTest {
    private static final String KEY = "key";
    private static final String TEST_CERT_FILE = "testCert.pem";

    private HashicorpCertificateResolver certificateResolver;
    private HashicorpVault vault;

    @BeforeEach
    void setup() {
        vault = mock(HashicorpVault.class);
        final Monitor monitor = mock(Monitor.class);
        certificateResolver = new HashicorpCertificateResolver(vault, monitor);
    }

    @Test
    void resolveCertificate() throws RuntimeException, IOException {
        var classloader = Thread.currentThread().getContextClassLoader();
        var pemExpected =  new String(Objects.requireNonNull(classloader.getResourceAsStream(TEST_CERT_FILE)).readAllBytes());
        when(vault.resolveSecret(KEY)).thenReturn(pemExpected);

        var certificate = Objects.requireNonNull(certificateResolver.resolveCertificate(KEY));
        var pemReceived = PemUtil.convertCertificateToPem(certificate);

        verify(vault, times(1)).resolveSecret(KEY);
        Assertions.assertEquals(pemExpected, pemReceived);
    }
}
