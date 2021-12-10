package org.eclipse.dataspaceconnector.junit.launcher;

import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.security.VaultResponse;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockVault implements Vault {
    private final Map<String, String> secrets = new ConcurrentHashMap<>();

    @Override
    public @Nullable
    String resolveSecret(String key) {
        return secrets.get(key);
    }

    @Override
    public VaultResponse storeSecret(String key, String value) {
        secrets.put(key, value);
        return VaultResponse.OK;
    }

    @Override
    public VaultResponse deleteSecret(String key) {
        secrets.remove(key);
        return VaultResponse.OK;
    }
}
