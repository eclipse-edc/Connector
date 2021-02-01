package com.microsoft.dagx.ids.transfer.core;

import com.microsoft.dagx.spi.transfer.TransferManager;
import com.microsoft.dagx.spi.transfer.TransferManagerRegistry;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * The default transfer registry.
 */
public class TransferManagerRegistryImpl implements TransferManagerRegistry {
    private List<TransferManager> managers = new ArrayList<>();

    @Override
    public void register(TransferManager manager) {
        managers.add(manager);
    }

    @Override
    public @Nullable TransferManager getManager(URI dataUrn) {
        for (TransferManager manager : managers) {
            if (manager.canHandle(dataUrn)) {
                return manager;
            }
        }
        ;
        return null;
    }
}
