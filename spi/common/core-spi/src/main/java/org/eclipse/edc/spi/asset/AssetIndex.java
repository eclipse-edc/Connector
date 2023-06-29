/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       ZF Friedrichshafen AG - added private property support
 *
 */

package org.eclipse.edc.spi.asset;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.spi.types.domain.asset.AssetEntry;

import java.util.List;
import java.util.stream.Stream;

/**
 * Query interface for {@link Asset} objects.
 * <br>
 * The EDC references {@code ContractOffer} and {@code TransferProcess}
 * to an {@link Asset} by its corresponding asset id. Therefore, it is absolutely crucial that assets are not removed from the {@link AssetIndex} as long as data transfers or contracts (agreements and offers) exists for them.
 * Additionally, as an {@link Asset} may be referenced by a contract, the content of an {@link Asset} and its corresponding data must not change in ways, that violates an existing contract.
 */
@ExtensionPoint
public interface AssetIndex extends DataAddressResolver {

    String ASSET_EXISTS_TEMPLATE = "Asset with ID %s already exists";
    String ASSET_NOT_FOUND_TEMPLATE = "Asset with ID %s not found";
    String DATA_ADDRESS_NOT_FOUND_TEMPLATE = "DataAddress with ID %s not found";
    String DUPLICATE_PROPERTY_KEYS_TEMPLATE = "Duplicate keys in properties and private properties are not allowed";

    /**
     * Finds all assets that are covered by a specific {@link QuerySpec}. Results are always sorted. If no {@link QuerySpec#getSortField()}
     * is specified, results are not explicitly sorted.
     * <p>
     * The general order of precedence of the query parameters is:
     * <pre>
     * filter &gt; sort &gt; limit
     * </pre>
     * <p>
     *
     * @param querySpec The query spec, e.g. paging, filtering, etc.
     * @return A potentially empty collection of {@link Asset}, never null.
     */
    Stream<Asset> queryAssets(QuerySpec querySpec);

    /**
     * Fetches the {@link Asset} with the given ID from the metadata backend.
     *
     * @param assetId A String that represents the Asset ID, in most cases this will be a UUID.
     * @return The {@link Asset} if one was found, or null otherwise.
     * @throws NullPointerException If {@code assetId} was null or empty.
     */
    Asset findById(String assetId);

    /**
     * Stores an {@link Asset} and a {@link DataAddress} in the asset index, if no asset with the same ID already exists.
     * Implementors must ensure that bothe objects are stored in a transactional way, and the DataAddress must always be resolvable for an Asset.
     *
     * @param asset       The {@link Asset} to store
     * @param dataAddress The {@link DataAddress} to store
     * @return {@link StoreResult#success()} if the objects were stored, {@link StoreResult#alreadyExists(String)} when an object with the same ID already exists.
     * @deprecated please use {@link #create(Asset)}
     */
    @Deprecated(since = "0.1.2", forRemoval = true)
    default StoreResult<Void> create(Asset asset, DataAddress dataAddress) {
        return create(new AssetEntry(asset, dataAddress));
    }

    /**
     * This method will be removed in favor of {@link #create(Asset)}
     *
     * @param item the asset entry.
     * @return the result.
     * @deprecated please use and override {@link #create(Asset)}
     */
    @Deprecated(since = "0.1.2")
    default StoreResult<Void> create(AssetEntry item) {
        var asset = item.getAsset();
        var assetWithDataAddress = asset.toBuilder()
                .dataAddress(item.getDataAddress())
                .build();
        return create(assetWithDataAddress);
    }

    /**
     * Stores a {@link Asset} in the asset index, if no asset with the same ID already exists.
     * Implementors must ensure that it's stored in a transactional way.
     *
     * @param asset       The {@link Asset} to store
     * @return {@link StoreResult#success()} if the objects were stored, {@link StoreResult#alreadyExists(String)} when an object with the same ID already exists.
     */
    default StoreResult<Void> create(Asset asset) {
        return create(new AssetEntry(asset, asset.getDataAddress()));
    }

    /**
     * Deletes an asset if it exists.
     *
     * @param assetId Id of the asset to be deleted.
     * @return {@link StoreResult#success(Object)} if the object was deleted, {@link StoreResult#notFound(String)} when an object with that ID was not found.
     * @throws EdcPersistenceException if something goes wrong.
     */
    StoreResult<Asset> deleteById(String assetId);

    /**
     * Counts all assets that are selected by the given criteria
     *
     * @param criteria the criterion lista
     * @return the number of assets (potentially 0)
     */
    long countAssets(List<Criterion> criteria);

    /**
     * Updates an asset with the content from the given {@link Asset}. If the asset is not found, no further database interaction takes place.
     *
     * @param asset The Asset containing the new values. ID will be ignored.
     * @return {@link StoreResult#success(Object)} if the object was updated, {@link StoreResult#notFound(String)} when an object with that ID was not found.
     */
    StoreResult<Asset> updateAsset(Asset asset);

    /**
     * Updates a {@link DataAddress} that is associated with the {@link Asset} that is identified by the {@code assetId} argument.
     * If the asset is not found, no further database interaction takes place.
     *
     * @param assetId     the database of the Asset to update
     * @param dataAddress The DataAddress containing the new values.
     * @return {@link StoreResult#success(Object)} if the object was updated, {@link StoreResult#notFound(String)} when an object with that ID was not found.
     */
    StoreResult<DataAddress> updateDataAddress(String assetId, DataAddress dataAddress);
}
