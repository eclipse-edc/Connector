package com.microsoft.dagx.transfer.store.memory;

import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 *
 */
public class InMemoryTransferProcessStore implements TransferProcessStore {

    @Override
    public @Nullable TransferProcess find(String id) {
        return null;
    }

    @Override
    public @NotNull List<TransferProcess> nextForState(int state, int max) {
        return null;
    }

    @Override
    public void create(TransferProcess process) {

    }

    @Override
    public void update(TransferProcess process) {

    }

    @Override
    public void delete(String processId) {

    }

    @Override
    public void createData(String processId, String key, Object data) {

    }

    @Override
    public void updateData(String processId, String key, Object data) {

    }

    @Override
    public void deleteData(String processId, String key) {

    }

    @Override
    public void deleteData(String processId, Set<String> keys) {

    }

    @Override
    public <T> T findData(Class<T> type, String processId, String resourceDefinitionId) {
        return null;
    }
}
