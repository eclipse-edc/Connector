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

import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

import java.util.stream.Stream;

/**
 * Query interface for {@link Asset} objects.
 */
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
     * Fetches the {@link Asset} with the given ID from the metadata backend.
     *
     * @param assetId A String that represents the Asset ID, in most cases this will be a UUID.
     * @return The {@link Asset} if one was found, or null otherwise.
     * @throws NullPointerException If {@code assetId} was null or empty.
     */
    Asset findById(String assetId);

    /**
     * Resolves a {@link DataAddress} for a given {@code Asset}. A {@code DataAddress} can be understood as a pointer into
     * a storage system like a database or a document store.
     *
     * @param assetId The {@code assetId} for which the data pointer should be fetched.
     * @return A DataAddress
     * @throws NullPointerException     if {@code asset} was null
     * @throws IllegalArgumentException if no corresponding {@code DataAddress} was found for a certain asset
     */
    DataAddress resolveForAsset(String assetId);
}
