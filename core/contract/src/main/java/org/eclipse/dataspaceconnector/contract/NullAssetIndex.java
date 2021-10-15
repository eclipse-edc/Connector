package org.eclipse.dataspaceconnector.contract;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.stream.Stream;

/**
 * NullObject of the {@link AssetIndex}
 */
public class NullAssetIndex implements AssetIndex {

    @Override
    public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
        return Stream.empty();
    }

    @Override
    public Asset findById(String assetId) {
        return null;
    }
}
