package org.eclipse.dataspaceconnector.spi.asset;

import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

import java.util.Map;

public interface AssetIndexLoader {
    void insert(Asset asset, DataAddress address);

    void insertAll(Map<Asset, DataAddress> entries);
}
