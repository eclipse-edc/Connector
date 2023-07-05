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

import org.eclipse.edc.connector.asset.spi.observe.AssetObservable;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.service.query.QueryValidator;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.dataaddress.DataAddressValidator;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;
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
        return create(asset.toBuilder().dataAddress(dataAddress).build());
    }

    @Override
    public ServiceResult<Asset> create(Asset asset) {
        var validDataAddress = dataAddressValidator.validate(asset.getDataAddress());
        if (validDataAddress.failed()) {
            return ServiceResult.badRequest(validDataAddress.getFailureMessages());
        }

        return transactionContext.execute(() -> {
            var createResult = index.create(asset);
            if (createResult.succeeded()) {
                observable.invokeForEach(l -> l.created(asset));
                return ServiceResult.success(asset);
            }
            return ServiceResult.fromFailure(createResult);
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
            deleted.onSuccess(a -> observable.invokeForEach(l -> l.deleted(a)));
            return ServiceResult.from(deleted);
        });
    }

    @Override
    public ServiceResult<Asset> update(Asset asset) {
        return transactionContext.execute(() -> {
            var updatedAsset = index.updateAsset(asset);
            updatedAsset.onSuccess(a -> observable.invokeForEach(l -> l.updated(a)));
            return ServiceResult.from(updatedAsset);
        });
    }

    @Override
    public ServiceResult<DataAddress> update(String assetId, DataAddress dataAddress) {
        return transactionContext.execute(() -> {
            var result = index.updateDataAddress(assetId, dataAddress);
            result.onSuccess(da -> observable.invokeForEach(l -> l.updated(findById(assetId))));
            return ServiceResult.from(result);
        });
    }
}
