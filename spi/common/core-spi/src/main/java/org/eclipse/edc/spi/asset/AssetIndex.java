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
import org.jetbrains.annotations.Nullable;

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

    /**
     * Returns all {@link Asset} objects that are selected by a certain expression
     *
     * @param expression An object containing a structured query to asset objects. If the expression contains no criteria,
     *                   {@code Stream.empty()} is returned. If {@link AssetSelectorExpression#SELECT_ALL} is passed in, all
     *                   Assets in the index are returned.
     * @return A {@code Stream} that contains all assets that are selected by the expression. Might be empty, never null.
     * @throws NullPointerException if the {@code AssetSelectorExpression} is null
     */
    Stream<Asset> queryAssets(AssetSelectorExpression expression);

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
    @Nullable
    Asset findById(String assetId);

    default StoreResult<Void> accept(Asset asset, DataAddress dataAddress) {
        return accept(new AssetEntry(asset, dataAddress));
    }

    StoreResult<Void> accept(AssetEntry item);

    /**
     * Deletes an asset.
     *
     * @param assetId Id of the asset to be deleted.
     * @return Deleted Asset or null if asset did not exist.
     * @throws EdcPersistenceException if something goes wrong.
     */
    StoreResult<Asset> deleteById(String assetId);

    /**
     * Counts all assets that are selected by the given criteria
     *
     * @param criteria the criterion list
     * @return the number of assets (potentially 0)
     */
    long countAssets(List<Criterion> criteria);

    /**
     * Updates an asset with the content from the given {@link Asset}. If the asset is not found, no further database interaction takes place.
     *
     * @param asset The Asset containing the new values. ID will be ignored.
     * @return the updated Asset, or null if the asset does not exist
     */
    StoreResult<Asset> updateAsset(Asset asset);

    /**
     * Updates a {@link DataAddress} that is associated with the {@link Asset} that is identified by the {@code assetId} argument.
     * If the asset is not found, no further database interaction takes place.
     *
     * @param assetId     the database of the Asset to update
     * @param dataAddress The DataAddress containing the new values.
     * @return the updated DataAddress
     */
    StoreResult<DataAddress> updateDataAddress(String assetId, DataAddress dataAddress);
}
