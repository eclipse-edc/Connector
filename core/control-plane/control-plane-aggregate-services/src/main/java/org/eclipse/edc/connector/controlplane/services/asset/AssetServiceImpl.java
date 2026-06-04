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

package org.eclipse.edc.connector.controlplane.services.asset;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.asset.spi.observe.AssetObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidator;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;
import java.util.function.Predicate;

import static java.lang.String.format;

public class AssetServiceImpl implements AssetService {

    private static final String ASSET_ID_QUERY = "contractAgreement.assetId";
    private static final String DUPLICATED_KEYS_MESSAGE = "Duplicate keys in properties and private properties are not allowed";
    private final AssetIndex index;
    private final ContractNegotiationStore contractNegotiationStore;
    private final TransactionContext transactionContext;
    private final AssetObservable observable;
    private final QueryValidator queryValidator;
    private final Monitor monitor;

    public AssetServiceImpl(AssetIndex index, ContractNegotiationStore contractNegotiationStore,
                            TransactionContext transactionContext, AssetObservable observable,
                            QueryValidator queryValidator, Monitor monitor) {
        this.index = index;
        this.contractNegotiationStore = contractNegotiationStore;
        this.transactionContext = transactionContext;
        this.observable = observable;
        this.queryValidator = queryValidator;
        this.monitor = monitor;
    }

    @Override
    public Asset findById(String assetId) {
        return transactionContext.execute(() -> index.findById(assetId));
    }

    @Override
    public ServiceResult<List<Asset>> search(QuerySpec query) {
        return queryValidator.validate(query)
                .flatMap(validation -> validation.failed()
                        ? ServiceResult.badRequest(validation.getFailureMessages())
                        : ServiceResult.success(queryAssets(query))
                );
    }

    @Override
    public ServiceResult<Asset> create(Asset asset) {
        if (asset.hasDuplicatePropertyKeys()) {
            return ServiceResult.badRequest(DUPLICATED_KEYS_MESSAGE);
        }

        logWarningWhenAssetCatalogPropertiesAreNotSet(asset);

        return transactionContext.execute(() ->
                index.create(asset)
                        .onSuccess(i -> observable.invokeForEach(l -> l.created(asset)))
                        .flatMap(ServiceResult::from)
                        .map(i -> asset)
        );
    }

    @Override
    public ServiceResult<Asset> delete(String assetId) {
        return transactionContext.execute(() -> {

            var query = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion(ASSET_ID_QUERY, "=", assetId)))
                    .build();

            try (var negotiationsOnAsset = contractNegotiationStore.queryNegotiations(query)) {
                if (negotiationsOnAsset.anyMatch(new AssetLockedPredicate())) {
                    return ServiceResult.conflict(format("Asset %s cannot be deleted as it is referenced by at least one contract agreement or an ongoing negotiation", assetId));
                }
            }

            var deleted = index.deleteById(assetId);
            deleted.onSuccess(a -> observable.invokeForEach(l -> l.deleted(a)));
            return ServiceResult.from(deleted);
        });
    }

    @Override
    public ServiceResult<Asset> update(Asset asset) {
        if (asset.hasDuplicatePropertyKeys()) {
            return ServiceResult.badRequest(DUPLICATED_KEYS_MESSAGE);
        }

        logWarningWhenAssetCatalogPropertiesAreNotSet(asset);

        return transactionContext.execute(() ->
                index.updateAsset(asset)
                        .onSuccess(a -> observable.invokeForEach(l -> l.updated(a)))
                        .flatMap(ServiceResult::from)
        );
    }

    @Deprecated(since = "management-api:v4")
    private void logWarningWhenAssetCatalogPropertiesAreNotSet(Asset asset) {
        if (asset.isCatalog() && (asset.getCatalogUrl() == null || asset.getCatalogFormat() == null)) {
            monitor.warning("The 'CatalogAsset' type is expecting 'catalogUrl' and 'catalogFormat' properties," +
                    "please adapt your clients accordingly");
        }
    }

    private List<Asset> queryAssets(QuerySpec query) {
        return transactionContext.execute(() -> {
            try (var stream = index.queryAssets(query)) {
                return stream.toList();
            }
        });
    }

    private static final class AssetLockedPredicate implements Predicate<ContractNegotiation> {

        @Override
        public boolean test(ContractNegotiation contractNegotiation) {
            return contractNegotiation.getContractAgreement() != null || !ContractNegotiationStates.isFinal(contractNegotiation.getState());
        }
    }

}
