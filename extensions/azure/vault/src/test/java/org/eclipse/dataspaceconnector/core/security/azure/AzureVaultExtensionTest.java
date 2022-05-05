/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.core.security.azure;

import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

class AzureVaultExtensionTest {

    private static final String CLIENT_ID = UUID.randomUUID().toString();
    private static final String TENANT_ID = UUID.randomUUID().toString();
    private static final String KEYVAULT_NAME = UUID.randomUUID().toString();
    private static final String CLIENT_SECRET = UUID.randomUUID().toString();
    private static final String CERTIFICATE_PATH = UUID.randomUUID().toString();
    private static final long TIMEOUT_MS = 500;
    private Monitor monitor;
    private AzureVaultExtension extension;

    @BeforeEach
    public void setUp() {
        monitor = new ConsoleMonitor();
        extension = new AzureVaultExtension();
    }

    @Test
    void neitherSecretOrCertificateProvided_shouldThrowException() {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.vault.clientid", CLIENT_ID,
                "edc.vault.name", KEYVAULT_NAME,
                "edc.vault.tenantid", TENANT_ID
        ));
        assertThrows(AzureVaultException.class, () -> extension.initialize(monitor, config));
    }

    @Test
    void onlyCertificateProvided_authenticateWithCertificate() throws InterruptedException {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.vault.clientid", CLIENT_ID,
                "edc.vault.name", KEYVAULT_NAME,
                "edc.vault.tenantid", TENANT_ID,
                "edc.vault.certificate", CERTIFICATE_PATH
        ));

        var l = new CountDownLatch(1);

        try (MockedStatic<AzureVault> utilities = mockStatic(AzureVault.class)) {
            utilities.when(() -> AzureVault.authenticateWithCertificate(monitor, CLIENT_ID, TENANT_ID, CERTIFICATE_PATH, KEYVAULT_NAME))
                    .then(i -> {
                        l.countDown();
                        return null;
                    });

            extension.initialize(monitor, config);

            assertThat(l.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue();
        }
    }

    @Test
    void onlySecretProvided_authenticateWithSecret() throws InterruptedException {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.vault.clientid", CLIENT_ID,
                "edc.vault.name", KEYVAULT_NAME,
                "edc.vault.tenantid", TENANT_ID,
                "edc.vault.clientsecret", CLIENT_SECRET
        ));
        var l = new CountDownLatch(1);

        try (MockedStatic<AzureVault> utilities = mockStatic(AzureVault.class)) {
            utilities.when(() -> AzureVault.authenticateWithSecret(monitor, CLIENT_ID, TENANT_ID, CLIENT_SECRET, KEYVAULT_NAME))
                    .then(i -> {
                        l.countDown();
                        return null;
                    });

            extension.initialize(monitor, config);

            assertThat(l.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue();
        }
    }

    @Test
    void bothSecretAndCertificateProvided_authenticateWithCertificate() throws InterruptedException {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.vault.clientid", CLIENT_ID,
                "edc.vault.name", KEYVAULT_NAME,
                "edc.vault.tenantid", TENANT_ID,
                "edc.vault.certificate", CERTIFICATE_PATH,
                "edc.vault.clientsecret", CLIENT_SECRET
        ));
        var l = new CountDownLatch(1);

        try (MockedStatic<AzureVault> utilities = mockStatic(AzureVault.class)) {
            utilities.when(() -> AzureVault.authenticateWithCertificate(monitor, CLIENT_ID, TENANT_ID, CERTIFICATE_PATH, KEYVAULT_NAME))
                    .then(i -> {
                        l.countDown();
                        return null;
                    });

            extension.initialize(monitor, config);

            assertThat(l.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue();
        }
    }
}
