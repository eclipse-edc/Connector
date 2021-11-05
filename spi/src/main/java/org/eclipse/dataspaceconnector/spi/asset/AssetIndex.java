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

/**
 * Query interface for {@link Asset} objects.
 */
public interface AssetIndex {

    String FEATURE = "edc:asset:assetindex";

    /**
     * Returns all {@link Asset} objects that are selected by the query
     *
     * @param query An object containing all query parameters.
     * @return A {@link AssetIndexResult} that contains all assets that are selected by the expression, the expression itself and a cursor for continuation.The {@code stream} might be empty, never null.
     * @throws NullPointerException if the {@code AssetSelectorExpression} is null
     */
    AssetIndexResult queryAssets(AssetIndexQuery query);

    /**
     * Fetches the {@link Asset} with the given ID from the metadata backend.
     *
     * @param assetId A String that represents the Asset ID, in most cases this will be a UUID.
     * @return The {@link Asset} if one was found, or null otherwise.
     * @throws NullPointerException If {@code assetId} was null or empty.
     */
    Asset findById(String assetId);

}
