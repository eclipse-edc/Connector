package org.eclipse.dataspaceconnector.extensions.api;

import org.eclipse.dataspaceconnector.api.datamanagement.asset.service.AssetService;
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.sql.asset.index.SqlAssetIndex;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class AssetServiceImplementation implements AssetService {

    private final SqlAssetIndex index;
    private final AssetLoader loader;
    private final ContractNegotiationStore contractNegotiationStore;
    private final TransactionContext transactionContext;

    public AssetServiceImplementation(SqlAssetIndex index, AssetLoader loader, ContractNegotiationStore contractNegotiationStore, TransactionContext transactionContext) {
        this.index = index;
        this.loader = loader;
        this.contractNegotiationStore = contractNegotiationStore;
        this.transactionContext = transactionContext;
    }

    @Override
    public Asset findById(String assetId) {
        return transactionContext.execute(() -> index.findById(assetId));
    }

    @Override
    public Collection<Asset> query(QuerySpec query) {
        return transactionContext.execute(() -> index.queryAssets(query).collect(toList()));
    }

    @Override
    public ServiceResult<Asset> delete(String assetId) {
        return transactionContext.execute(() -> {
            var filter = format("contractAgreement.assetId = %s", assetId);
            var query = QuerySpec.Builder.newInstance().filter(filter).build();

            var negotiationsOnAsset = contractNegotiationStore.queryNegotiations(query);
            if (negotiationsOnAsset.findAny().isPresent()) {
                return ServiceResult.conflict(format("Asset %s cannot be deleted as it is referenced by at least one contract agreement", assetId));
            }

            var deleted = loader.deleteById(assetId);
            if (deleted == null) {
                return ServiceResult.notFound(format("Asset %s does not exist", assetId));
            }

            return ServiceResult.success(deleted);
        });
    }

    @Override
    public ServiceResult<Asset> create(Asset asset, DataAddress dataAddress) {
        return transactionContext.execute(() -> {
            if (findById(asset.getId()) == null) {
                loader.accept(asset, dataAddress);
                return ServiceResult.success(asset);
            } else {
                return ServiceResult.conflict(format("Asset %s cannot be created because it already exist", asset.getId()));
            }
        });
    }
}
