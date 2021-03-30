package com.microsoft.dagx.transfer.core.provision;

import com.microsoft.dagx.spi.transfer.provision.ProvisionContext;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Provisioned context backed by {@link TransferProcessStore}.
 */
public class ProvisionContextImpl implements ProvisionContext {
    private TransferProcessStore processStore;
    private final Consumer<ProvisionedResource> callback;

    public ProvisionContextImpl(TransferProcessStore processStore, Consumer<ProvisionedResource> callback) {
        this.processStore = processStore;
        this.callback = callback;
    }

    @Override
    public void callback(ProvisionedResource resource) {
        callback.accept(resource);
    }

    @Override
    public void create(String processId, String resourceDefinitionId, Object data) {
        processStore.createData(processId, resourceDefinitionId, data);
    }

    @Override
    public void update(String processId, String resourceDefinitionId, Object data) {
        processStore.updateData(processId, resourceDefinitionId, data);
    }

    @Override
    public <T> @Nullable T find(Class<T> type, String processId, String resourceDefinitionId) {
        return processStore.findData(type, processId, resourceDefinitionId);
    }
}
