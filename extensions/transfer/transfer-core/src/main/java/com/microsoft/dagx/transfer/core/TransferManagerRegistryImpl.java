package com.microsoft.dagx.transfer.core;

import com.microsoft.dagx.spi.transfer.TransferManager;
import com.microsoft.dagx.spi.transfer.TransferManagerRegistry;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.Nullable;

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
    public @Nullable TransferManager getManager(DataRequest dataRequest) {
        for (TransferManager manager : managers) {
            if (manager.canHandle(dataRequest)) {
                return manager;
            }
        }
        ;
        return null;
    }
}
