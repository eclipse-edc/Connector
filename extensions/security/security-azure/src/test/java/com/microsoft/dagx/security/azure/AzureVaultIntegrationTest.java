package com.microsoft.dagx.security.azure;

import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.microsoft.dagx.security.azure.mgmt.TestResourceManager;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.VaultResponse;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class AzureVaultIntegrationTest {

    private static TestResourceManager resourceManager;
    private static ResourceGroup rg;
    private static AzureVault azureVault;
    private String secretKey;

    @BeforeAll
    public static void setupAzure() throws IOException {
        var resStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("azurecredentials.properties");
        var props = new Properties();
        props.load(resStream);

        var clientId = props.getProperty("clientId");
        var tenantId = props.getProperty("tenantId");
        var clientSecret = props.getProperty("clientSecret");
        var subsciptionId = props.getProperty("subscriptionId");


        resourceManager = new TestResourceManager(clientId, tenantId, clientSecret, subsciptionId);

        rg = resourceManager.createRandomResourceGroup();

        Vault vault = resourceManager.deployVault(rg, clientId);

        azureVault = AzureVault.authenticateWithSecret(new LoggerMonitor(), clientId, tenantId, clientSecret, vault.name());
    }

    @BeforeEach
    void verifyAzureResourceGroup() {
        secretKey = "testkey";
    }

    @Test
    void storeSecret() {

        VaultResponse vaultResponse = azureVault.storeSecret(secretKey, "testvalue");

        assertTrue(vaultResponse.success());
        assertEquals("testvalue", azureVault.resolveSecret(secretKey));
    }

    @Test
    void storeSecret_overwrites() {
        azureVault.storeSecret(secretKey, "value1");
        azureVault.storeSecret(secretKey, "value2");

        assertEquals("value2", azureVault.resolveSecret(secretKey));
    }

    @Test
    void resolveSecret_notExist() {
        assertNull(azureVault.resolveSecret("notexist"));
    }

    @Test
    void resolveSecret() {
        assertTrue(azureVault.storeSecret(secretKey, "someVal").success());

        assertEquals("someVal", azureVault.resolveSecret(secretKey));
    }

    @Test
    void delete_notExist() {
        VaultResponse vr = azureVault.deleteSecret("notexist");
        assertFalse(vr.success());
        assertNotNull(vr.error());
    }

    @Test
    void delete() {
        azureVault.storeSecret(secretKey, "someval");

        VaultResponse vr = azureVault.deleteSecret(secretKey);
        assertTrue(vr.success());
        assertNull(vr.error());
    }

    @AfterEach
    void cleanup() {
        azureVault.deleteSecret(secretKey);
    }

    @AfterAll
    public static void cleanupAzure() {
        resourceManager.deleteResourceGroup(rg);
    }


    private static class LoggerMonitor implements Monitor {
        private final Logger logger = LoggerFactory.getLogger(LoggerMonitor.class);

        @Override
        public void info(String message, Throwable... errors) {
            if (errors == null || errors.length == 0)
                logger.info(message);
            else
                logger.error(message);
        }
    }
}
