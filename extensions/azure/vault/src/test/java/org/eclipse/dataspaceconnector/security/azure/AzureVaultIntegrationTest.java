/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.security.azure;

import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.VaultResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@IntegrationTest
public class AzureVaultIntegrationTest {

    private static final String ID = UUID.randomUUID().toString();
    private static final String SECRET_KEY_1 = "testkey1" + ID;
    private static final String SECRET_KEY_2 = "testkey2" + ID;
    private static final String SECRET_KEY_3 = "testkey3" + ID;
    private static final String SECRET_KEY_4 = "testkey4" + ID;
    private static AzureVault azureVault;

    @AfterAll
    static void verifyAzureResourceGroup() {
        azureVault.deleteSecret(SECRET_KEY_1);
        azureVault.deleteSecret(SECRET_KEY_2);
        azureVault.deleteSecret(SECRET_KEY_3);
        azureVault.deleteSecret(SECRET_KEY_4);
    }

    @BeforeEach
    public void setupAzure() throws IOException, URISyntaxException {
        var resStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("azurecredentials.properties");
        var props = new Properties();
        props.load(resStream);

        var clientId = props.getProperty("edc.vault.clientid");
        var tenantId = props.getProperty("edc.vault.tenantid");
        var vaultName = props.getProperty("edc.vault.name");
        var certfile = props.getProperty("edc.vault.certificate");
        var certPath = Thread.currentThread().getContextClassLoader().getResource(certfile).toURI().getPath();

        azureVault = AzureVault.authenticateWithCertificate(new LoggerMonitor(), clientId, tenantId, certPath, vaultName);

    }

    @Test
    void storeSecret() {

        VaultResponse vaultResponse = azureVault.storeSecret(SECRET_KEY_1, "testvalue");

        assertTrue(vaultResponse.success());
        assertEquals("testvalue", azureVault.resolveSecret(SECRET_KEY_1));
    }

    @Test
    void storeSecret_overwrites() {
        azureVault.storeSecret(SECRET_KEY_2, "value1");
        azureVault.storeSecret(SECRET_KEY_2, "value2");

        assertEquals("value2", azureVault.resolveSecret(SECRET_KEY_2));
    }

    @Test
    void resolveSecret_notExist() {
        assertNull(azureVault.resolveSecret("notexist"));
    }

    @Test
    void resolveSecret() {
        assertTrue(azureVault.storeSecret(SECRET_KEY_3, "someVal").success());

        assertEquals("someVal", azureVault.resolveSecret(SECRET_KEY_3));
    }

    @Test
    void delete_notExist() {
        VaultResponse vr = azureVault.deleteSecret("notexist");
        assertFalse(vr.success());
        assertNotNull(vr.error());
    }

    @Test
    void delete() {
        azureVault.storeSecret(SECRET_KEY_4, "someval");

        VaultResponse vr = azureVault.deleteSecret(SECRET_KEY_4);
        assertTrue(vr.success());
        assertNull(vr.error());
    }

    private static class LoggerMonitor implements Monitor {
        private final Logger logger = LoggerFactory.getLogger(LoggerMonitor.class);

        @Override
        public void info(String message, Throwable... errors) {
            if (errors == null || errors.length == 0) {
                logger.info(message);
            } else {
                logger.error(message);
            }
        }
    }
}
