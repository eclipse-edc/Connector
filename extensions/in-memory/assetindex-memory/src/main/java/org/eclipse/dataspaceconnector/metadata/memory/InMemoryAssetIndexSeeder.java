package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

public class InMemoryAssetIndexSeeder {

    private final InMemoryAssetIndex index;

    public InMemoryAssetIndexSeeder(InMemoryAssetIndex index) {
        this.index = index;
    }

    public void addAssets(Asset asset, DataAddress entry) {
        index.add(asset, entry);
    }
}
