package com.microsoft.dagx.security.azure;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.CertificateResolver;
import com.microsoft.dagx.spi.security.PrivateKeyResolver;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.VaultExtension;

import java.util.Objects;
import java.util.stream.Stream;

import static com.microsoft.dagx.spi.util.ConfigurationFunctions.propOrEnv;


public class AzureVaultExtension implements VaultExtension {

    private Vault vault;

    @Override
    public void initialize(Monitor monitor) {

        String clientId = propOrEnv("dagx.vault.clientid", null);
        String tenantId = propOrEnv("dagx.vault.tenantid", null);
        String certPath = propOrEnv("dagx.vault.certificate", null);
        String keyVaultName = propOrEnv("dagx.vault.name", null);

        if (Stream.of(clientId, tenantId, certPath, keyVaultName).anyMatch(Objects::isNull))
            throw new AzureVaultException("Please supply all of dagx.vault.clientid, dagx.vault.tenantid, dagx.vault.certificate and dagx.vault.name");

        this.vault = AzureVault.authenticateWithCertificate(monitor, clientId, tenantId, certPath, keyVaultName);
    }

    @Override
    public Vault getVault() {
        return vault;
    }

    @Override
    public PrivateKeyResolver getPrivateKeyResolver() {
        return new AzurePrivateKeyResolver(vault);
    }

    @Override
    public CertificateResolver getCertificateResolver() {
        return new AzureCertificateResolver(vault);
    }
}
