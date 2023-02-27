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

package org.eclipse.edc.connector.service.asset;

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.service.query.QueryValidator;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.dataaddress.DataAddressValidator;
import org.eclipse.edc.spi.observe.asset.AssetObservable;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;

public class AssetServiceImpl implements AssetService {
    private static final String ASSET_ID_QUERY = "contractAgreement.assetId";
    private final AssetIndex index;
    private final ContractNegotiationStore contractNegotiationStore;
    private final TransactionContext transactionContext;
    private final AssetObservable observable;
    private final DataAddressValidator dataAddressValidator;
    private final QueryValidator queryValidator;

    public AssetServiceImpl(AssetIndex index, ContractNegotiationStore contractNegotiationStore,
                            TransactionContext transactionContext, AssetObservable observable,
                            DataAddressValidator dataAddressValidator) {
        this.index = index;
        this.contractNegotiationStore = contractNegotiationStore;
        this.transactionContext = transactionContext;
        this.observable = observable;
        this.dataAddressValidator = dataAddressValidator;
        queryValidator = new AssetQueryValidator();
    }

    @Override
    public Asset findById(String assetId) {
        return transactionContext.execute(() -> index.findById(assetId));
    }

    @Override
    public ServiceResult<Stream<Asset>> query(QuerySpec query) {
        var result = queryValidator.validate(query);

        if (result.failed()) {
            return ServiceResult.badRequest(result.getFailureMessages());
        }

        return ServiceResult.success(transactionContext.execute(() -> index.queryAssets(query)));
    }

    @Override
    public ServiceResult<Asset> create(Asset asset, DataAddress dataAddress) {
        var validDataAddress = dataAddressValidator.validate(dataAddress);
        if (validDataAddress.failed()) {
            return ServiceResult.badRequest(validDataAddress.getFailureMessages());
        }

        return transactionContext.execute(() -> {
            if (findById(asset.getId()) == null) {
                index.accept(asset, dataAddress);
                observable.invokeForEach(l -> l.created(asset));
                return ServiceResult.success(asset);
            } else {
                return ServiceResult.conflict(format("Asset %s cannot be created because it already exist", asset.getId()));
            }
        });
    }

    @Override
    public ServiceResult<Asset> delete(String assetId) {
        return transactionContext.execute(() -> {

            var query = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion(ASSET_ID_QUERY, "=", assetId)))
                    .build();

            try (var negotiationsOnAsset = contractNegotiationStore.queryNegotiations(query)) {
                if (negotiationsOnAsset.findAny().isPresent()) {
                    return ServiceResult.conflict(format("Asset %s cannot be deleted as it is referenced by at least one contract agreement", assetId));
                }
            }

            var deleted = index.deleteById(assetId);
            if (deleted == null) {
                return ServiceResult.notFound(format("Asset %s does not exist", assetId));
            }

            observable.invokeForEach(l -> l.deleted(deleted));
            return ServiceResult.success(deleted);
        });
    }

    @Override
    public ServiceResult<Void> update(String assetId, Asset asset) {
        if (!Objects.equals(assetId, asset.getId())) {
            return ServiceResult.badRequest("Asset.getId() must match assetId");
        }
        return transactionContext.execute(() -> {
            if (findById(assetId) == null) {
                return ServiceResult.notFound(format("Asset %s cannot be updated because it does not exist", assetId));
            }
            var updatedAsset = index.updateAsset(assetId, asset);
            observable.invokeForEach(l -> l.updated(updatedAsset));

            return ServiceResult.success();
        });
    }

    @Override
    public ServiceResult<Void> update(String assetId, DataAddress dataAddress) {
        return transactionContext.execute(() -> {
            var asset = findById(assetId);
            if (asset == null) {
                return ServiceResult.notFound(format("DataAddress for Asset ID= %s cannot be updated because it does not exist", assetId));
            }

            index.updateDataAddress(assetId, dataAddress);
            observable.invokeForEach(l -> l.updated(asset));

            return ServiceResult.success();
        });
    }
}
