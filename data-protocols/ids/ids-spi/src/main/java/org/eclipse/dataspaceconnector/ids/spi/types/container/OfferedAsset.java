package org.eclipse.dataspaceconnector.ids.spi.types.container;

import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Container object for an asset and its corresponding contract offers. Required for EDC to IDS transformation, as the IDS Resource may contain multiple IDS Contract Offers.
 */
public class OfferedAsset {
    private final List<ContractOffer> contractOffers;
    private final Asset asset;

    public OfferedAsset(@NotNull Asset asset, @NotNull List<ContractOffer> targetingContractOffers) {
        this.asset = Objects.requireNonNull(asset);
        this.contractOffers = Objects.requireNonNull(targetingContractOffers);
    }

    /**
     * All {@link ContractOffer} that contain the corresponding {@link Asset}.
     *
     * @return list of  {@link ContractOffer}
     */
    public List<ContractOffer> getTargetingContractOffers() {
        return contractOffers;
    }

    public Asset getAsset() {
        return asset;
    }
}
