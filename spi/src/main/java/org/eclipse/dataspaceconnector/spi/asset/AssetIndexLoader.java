package org.eclipse.dataspaceconnector.spi.asset;

import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

import java.util.List;
import java.util.Map;

public interface AssetIndexLoader {
    String FEATURE = "edc:asset:assetindex:loader";

    void insert(Asset asset, DataAddress address);

    void insertAll(List<Map.Entry<Asset, DataAddress>> entries);
}
