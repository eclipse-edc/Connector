/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.asset.service;

import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class AssetServiceImpl implements AssetService {
    private final AssetIndex index;
    private final AssetLoader loader;
    private final ContractNegotiationStore contractNegotiationStore;
    private final TransactionContext transactionContext;

    public AssetServiceImpl(AssetIndex index, AssetLoader loader, ContractNegotiationStore contractNegotiationStore, TransactionContext transactionContext) {
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
