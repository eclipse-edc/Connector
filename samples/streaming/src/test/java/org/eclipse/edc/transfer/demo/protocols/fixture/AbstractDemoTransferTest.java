package org.eclipse.edc.transfer.demo.protocols.fixture;

import org.eclipse.edc.common.testfixtures.EdcExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.security.VaultResponse;
import org.eclipse.edc.spi.transfer.TransferWaitStrategy;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for end-to-end demo protocol testing.
 */
@ExtendWith(EdcExtension.class)
abstract public class AbstractDemoTransferTest {

    /**
     * Fixture that obtains a reference to the runtime.
     *
     * @param extension the injected runtime instance
     */
    @BeforeEach
    protected void before(EdcExtension extension) {
        // register a mock Vault
        extension.registerServiceMock(Vault.class, new MockVault());

        // register a wait strategy of 1ms to speed up the interval between transfer manager iterations
        extension.registerServiceMock(TransferWaitStrategy.class, () -> 1);
    }

    protected static class MockVault implements Vault {
        private Map<String, String> secrets = new ConcurrentHashMap<>();

        @Override
        public @Nullable String resolveSecret(String key) {
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

}
