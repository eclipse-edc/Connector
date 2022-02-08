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

package org.eclipse.dataspaceconnector.spi.asset;

import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.system.Feature;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

/**
 * Query interface for {@link Asset} objects.
 * <br>
 * The EDC references {@link org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer} and {@link org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess}
 * to an {@link Asset} by its corresponding asset id. Therefore, it is absolutely crucial that assets are not removed from the {@link AssetIndex} as long as data transfers or contracts (agreements and offers) exists for them.
 * Additionally, as an {@link Asset} may be referenced by a contract, the content of an {@link Asset} and its corresponding data must not change in ways, that violates an existing contract.
 */
@Feature(AssetIndex.FEATURE)
public interface AssetIndex {

    String FEATURE = "edc:asset:assetindex";

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
     * Returns true if the set of asset are in the group of assets defined by criteria.
     */
    Stream<Asset> queryAssets(List<Criterion> criteria);

    /**
     * Fetches the {@link Asset} with the given ID from the metadata backend.
     *
     * @param assetId A String that represents the Asset ID, in most cases this will be a UUID.
     * @return The {@link Asset} if one was found, or null otherwise.
     * @throws NullPointerException If {@code assetId} was null or empty.
     */
    @Nullable
    Asset findById(String assetId);

}
