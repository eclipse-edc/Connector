package org.eclipse.dataspaceconnector.dataloading;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

public class AssetEntry {
    private final Asset asset;
    private final DataAddress dataAddress;

    @JsonCreator
    public AssetEntry(@JsonProperty("asset") Asset asset, @JsonProperty("dataAddress") DataAddress dataAddress) {
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
