package org.eclipse.dataspaceconnector.dataloading;

import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

public class AssetEntry {
    private final Asset asset;
    private final DataAddress dataAddress;

    public AssetEntry(Asset asset, DataAddress dataAddress) {
        this.asset = asset;
        this.dataAddress = dataAddress;
    }

    public Asset getAsset() {
        return asset;
    }

    public DataAddress getDataAddress() {
        return dataAddress;
    }
}
