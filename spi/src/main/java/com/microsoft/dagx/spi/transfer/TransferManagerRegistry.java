package com.microsoft.dagx.spi.transfer;

import org.jetbrains.annotations.Nullable;

import java.net.URI;

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
     * @param dataUrn the data to transfer
     */
    @Nullable
    TransferManager getManager(URI dataUrn);
}
