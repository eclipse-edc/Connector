package com.microsoft.dagx.spi.transfer.provision;

import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;
import org.jetbrains.annotations.Nullable;

/**
 * Execution context used by {@link Provisioner}s.
 */
public interface ProvisionContext {

    /**
     * Invoked when a provision request has completed.
     */
    void callback(ProvisionedResource resource);

    /**
     * Persists data related to a provision request.  Data will be removed after all resources have been provisioned for a transfer process.
     *
     * This is intended as a facility to use for recovery. For example, infrastructure request ids can be persisted and recovered to continue processing after runtime restart.
     */
    default void create(String processId, String resourceDefinitionId, Object data) {
        throw new UnsupportedOperationException();
    }

    /**
     * Updates data related to a provision request.
     */
    default void update(String processId, String resourceDefinitionId, Object data) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns data related to a provision request.
     */
    default @Nullable <T> T find(Class<T> type, String processId, String resourceDefinitionId) {
        throw new UnsupportedOperationException();
    }
}
