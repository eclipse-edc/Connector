/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.transfer.store;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Manages persistent storage of {@link TransferProcess} state.
 */
public interface TransferProcessStore {

    TransferProcess find(String id);

    @Nullable
    String processIdForTransferId(String id);

    @NotNull
    List<TransferProcess> nextForState(int state, int max);

    void create(TransferProcess process);

    void update(TransferProcess process);

    void delete(String processId);

    void createData(String processId, String key, Object data);

    void updateData(String processId, String key, Object data);

    void deleteData(String processId, String key);

    void deleteData(String processId, Set<String> keys);

    <T> T findData(Class<T> type, String processId, String resourceDefinitionId);

    void printState(Monitor monitor);
}
