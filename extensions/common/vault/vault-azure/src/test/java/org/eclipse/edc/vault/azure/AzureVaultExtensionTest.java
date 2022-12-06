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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.vault.azure;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class AzureVaultExtensionTest {

    private static final String CLIENT_ID = UUID.randomUUID().toString();
    private static final String TENANT_ID = UUID.randomUUID().toString();
    private static final String KEYVAULT_NAME = UUID.randomUUID().toString();
    private static final String CLIENT_SECRET = UUID.randomUUID().toString();
    private static final String CERTIFICATE_PATH = UUID.randomUUID().toString();

    private final Monitor monitor = mock(Monitor.class);
    private final ServiceExtensionContext context = mock(ServiceExtensionContext.class);

    private final AzureVaultExtension extension = new AzureVaultExtension();

    @BeforeEach
    public void setUp() {
        when(context.getSetting("edc.vault.clientid", null)).thenReturn(CLIENT_ID);
        when(context.getSetting("edc.vault.name", null)).thenReturn(KEYVAULT_NAME);
        when(context.getSetting("edc.vault.tenantid", null)).thenReturn(TENANT_ID);
        when(context.getMonitor()).thenReturn(monitor);
    }

    @Test
    void neitherSecretOrCertificateProvided_shouldThrowException() {
        assertThrows(AzureVaultException.class, () -> extension.initialize(context));
    }

    @Test
    void onlyCertificateProvided_authenticateWithCertificate() {
        try (MockedStatic<AzureVault> staticMock = mockStatic(AzureVault.class)) {
            when(context.getSetting("edc.vault.certificate", null)).thenReturn(CERTIFICATE_PATH);

            extension.initialize(context);

            staticMock.verify(
                    () -> AzureVault.authenticateWithCertificate(monitor, CLIENT_ID, TENANT_ID, CERTIFICATE_PATH, KEYVAULT_NAME),
                    times(1));
            staticMock.verifyNoMoreInteractions();
        }
    }

    @Test
    void onlySecretProvided_authenticateWithSecret() {
        try (var staticMock = mockStatic(AzureVault.class)) {
            when(context.getSetting("edc.vault.clientsecret", null)).thenReturn(CLIENT_SECRET);

            extension.initialize(context);

            staticMock.verify(
                    () -> AzureVault.authenticateWithSecret(monitor, CLIENT_ID, TENANT_ID, CLIENT_SECRET, KEYVAULT_NAME),
                    times(1));
            staticMock.verifyNoMoreInteractions();
        }
    }

    @Test
    void bothSecretAndCertificateProvided_authenticateWithCertificate() {
        try (MockedStatic<AzureVault> staticMock = mockStatic(AzureVault.class)) {
            when(context.getSetting("edc.vault.certificate", null)).thenReturn(CERTIFICATE_PATH);
            when(context.getSetting("edc.vault.clientsecret", null)).thenReturn(CLIENT_SECRET);

            extension.initialize(context);

            staticMock.verify(
                    () -> AzureVault.authenticateWithCertificate(monitor, CLIENT_ID, TENANT_ID, CERTIFICATE_PATH, KEYVAULT_NAME),
                    times(1));
            staticMock.verifyNoMoreInteractions();
        }
    }
}
