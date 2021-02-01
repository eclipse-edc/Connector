package com.microsoft.dagx.spi.transfer;

import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.Nullable;

/**
 * A registry of {@link TransferManager}.
 */
public interface TransferManagerRegistry {

    /**
     * Register the manager.
     */
    void register(TransferManager manager);

    /**
     * Returns the transfer manager if one is registered that can handle the data represented by the urn; otherwise null if no manager
     * is found.
     *
     * @param dataRequest the data to transfer
     */
    @Nullable
    TransferManager getManager(DataRequest dataRequest);
}
