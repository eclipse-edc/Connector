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

package org.eclipse.edc.connector.spi.asset;

import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;

import java.util.stream.Stream;

public interface AssetService {

    /**
     * Returns an asset by its id
     *
     * @param assetId id of the asset
     * @return the asset, null if it's not found
     */
    Asset findById(String assetId);

    /**
     * Query assets
     *
     * @param query request
     * @return the collection of assets that matches the query
     */
    ServiceResult<Stream<Asset>> query(QuerySpec query);

    /**
     * Create an asset with its related data address
     *
     * @param asset       the asset
     * @param dataAddress the address of the asset
     * @return successful result if the asset is created correctly, failure otherwise
     * @deprecated please use {@link #create(Asset)}
     */
    @Deprecated(since = "0.1.2")
    ServiceResult<Asset> create(Asset asset, DataAddress dataAddress);

    /**
     * Create an asset
     *
     * @param asset       the asset
     * @return successful result if the asset is created correctly, failure otherwise
     */
    ServiceResult<Asset> create(Asset asset);

    /**
     * Delete an asset
     *
     * @param assetId the id of the asset to be deleted
     * @return successful result if the asset is deleted correctly, failure otherwise
     */
    ServiceResult<Asset> delete(String assetId);

    /**
     * Updates an asset. If the asset does not yet exist, {@link ServiceResult#notFound(String)} will be returned.
     *
     * @param asset The content of the Asset. Note that {@link Asset#getId()} will be ignored, rather the separately supplied ID is used
     * @return successful if updated, a failure otherwise.
     */
    ServiceResult<Asset> update(Asset asset);

    /**
     * Updates a {@link DataAddress}. If the associated asset does not yet exist, {@link ServiceResult#notFound(String)} will be returned;
     *
     * @param assetId     The ID of the asset to update.
     * @param dataAddress The content of the DataAddress.
     * @return successful if updated, a failure otherwise.
     */
    ServiceResult<DataAddress> update(String assetId, DataAddress dataAddress);
}
