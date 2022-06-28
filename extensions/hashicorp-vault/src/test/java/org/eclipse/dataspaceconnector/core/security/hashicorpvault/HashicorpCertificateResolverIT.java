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
import org.eclipse.dataspaceconnector.common.util.junit.annotations.IntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

@IntegrationTest
@Tag("HashicorpVaultIntegrationTest")
class HashicorpCertificateResolverIT extends AbstractHashicorpIT {

    @Test
    @DisplayName("Resolve a valid certificate")
    void resolveCertificate_success() throws IOException {
        var key = UUID.randomUUID().toString();
        var certificateExpected = X509CertificateTestUtil.generateCertificate(5, "Test");
        var pem = X509CertificateTestUtil.convertToPem(certificateExpected);

        var vault = testExtension.getVault();
        vault.storeSecret(key, pem);
        var resolver = testExtension.getCertificateResolver();
        var certificateResult = resolver.resolveCertificate(key);

        Assertions.assertEquals(certificateExpected, certificateResult);
    }

    @Test
    @DisplayName("Fail resolving an invalid certificate")
    void resolveCertificate_malformed() {
        var key = UUID.randomUUID().toString();
        var value = UUID.randomUUID().toString();
        var vault = testExtension.getVault();
        vault.storeSecret(key, value);

        var resolver = testExtension.getCertificateResolver();
        var certificateResult = resolver.resolveCertificate(key);
        Assertions.assertNull(certificateResult);
    }
}
