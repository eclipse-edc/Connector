package com.microsoft.dagx.security.azure;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.VaultResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import static com.microsoft.dagx.spi.util.ConfigurationFunctions.propOrEnv;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
public class AzureVaultIntegrationTest {

    private static final String secretKey1 = "testkey1";
    private static final String secretKey2 = "testkey2";
    private static final String secretKey3 = "testkey3";
    private static final String secretKey4 = "testkey4";
    private static AzureVault azureVault;

    @BeforeAll
    public static void setupAzure() throws IOException, URISyntaxException {
        // this is necessary because the @EnabledIf... annotation does not prevent @BeforeAll to be called
        var isCi = propOrEnv("CI", "false");
        if (!Boolean.parseBoolean(isCi)) {
            return;
        }


        var resStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("azurecredentials.properties");
        var props = new Properties();
        props.load(resStream);

        var clientId = props.getProperty("dagx.vault.clientid");
        var tenantId = props.getProperty("dagx.vault.tenantid");
        var vaultName = props.getProperty("dagx.vault.name");
        var certfile = props.getProperty("dagx.vault.certificate");
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
