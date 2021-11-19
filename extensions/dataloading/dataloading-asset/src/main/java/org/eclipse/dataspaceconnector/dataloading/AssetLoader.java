package org.eclipse.dataspaceconnector.dataloading;

import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

public interface AssetLoader extends DataSink<AssetEntry> {
    String FEATURE = "edc:asset:assetindex:loader";

    void accept(Asset asset, DataAddress dataAddress);
}
