/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
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

import static org.junit.jupiter.api.Assertions.*;

@Disabled
@IntegrationTest
public class AzureVaultIntegrationTest {

    private static final String id = UUID.randomUUID().toString();
    private static final String secretKey1 = "testkey1" + id;
    private static final String secretKey2 = "testkey2" + id;
    private static final String secretKey3 = "testkey3" + id;
    private static final String secretKey4 = "testkey4" + id;
    private static AzureVault azureVault;

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

    @AfterAll
    static void verifyAzureResourceGroup() {
        azureVault.deleteSecret(secretKey1);
        azureVault.deleteSecret(secretKey2);
        azureVault.deleteSecret(secretKey3);
        azureVault.deleteSecret(secretKey4);
    }

    @Test
    void storeSecret() {

        VaultResponse vaultResponse = azureVault.storeSecret(secretKey1, "testvalue");

        assertTrue(vaultResponse.success());
        assertEquals("testvalue", azureVault.resolveSecret(secretKey1));
    }

    @Test
    void storeSecret_overwrites() {
        azureVault.storeSecret(secretKey2, "value1");
        azureVault.storeSecret(secretKey2, "value2");

        assertEquals("value2", azureVault.resolveSecret(secretKey2));
    }

    @Test
    void resolveSecret_notExist() {
        assertNull(azureVault.resolveSecret("notexist"));
    }

    @Test
    void resolveSecret() {
        assertTrue(azureVault.storeSecret(secretKey3, "someVal").success());

        assertEquals("someVal", azureVault.resolveSecret(secretKey3));
    }

    @Test
    void delete_notExist() {
        VaultResponse vr = azureVault.deleteSecret("notexist");
        assertFalse(vr.success());
        assertNotNull(vr.error());
    }

    @Test
    void delete() {
        azureVault.storeSecret(secretKey4, "someval");

        VaultResponse vr = azureVault.deleteSecret(secretKey4);
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
