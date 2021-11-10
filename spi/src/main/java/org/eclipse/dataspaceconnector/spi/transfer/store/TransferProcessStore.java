/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.transfer.store;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Manages persistent storage of {@link TransferProcess} state.
 */
public interface TransferProcessStore {

    String FEATURE = "dataspaceconnector:transferprocessstore";

    TransferProcess find(String id);

    @Nullable
    String processIdForTransferId(String id);

    /**
     * Returns a list of TransferProcesses that are in a specific state.
     * <br/>
     * Implementors MUST handle the starvation scenario, i.e. when the number of processes is greater than the number
     * passedin via {@code max}.
     * E.g. database-based implementations should perform a query along the lines of {@code SELECT ... ORDER BY TransferProcess#stateTimestamp}.
     * Then, after the check, users of this method must update the {@code TransferProcess#stateTimestamp} even if the process
     * remains unchanged.
     * Some database frameworks such as Spring have automatic lastChanged columns.
     *
     * @param state The state that the processes of interest should be in.
     * @param max   The maximum amount of result items.
     * @return A list of TransferProcesses (at most _max_) that are in the desired state.
     */
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

}
