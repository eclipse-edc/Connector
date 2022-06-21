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
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.UUID;

@IntegrationTest
@Tag("HashicorpVaultIntegrationTest")
class HashicorpCertificateResolverIT extends AbstractHashicorpIT {

    @Test
    void resolveCertificate_success() throws CertificateException, IOException, NoSuchAlgorithmException, OperatorCreationException {
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
