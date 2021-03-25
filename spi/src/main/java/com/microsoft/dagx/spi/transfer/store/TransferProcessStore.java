package com.microsoft.dagx.spi.transfer.store;

import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;

/**
 * Manages persistent storage of {@link TransferProcess} state.
 */
public interface TransferProcessStore {

    void create(TransferProcess process);

    void update(TransferProcess process);

    void delete(String processId);
}
