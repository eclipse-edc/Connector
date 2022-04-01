/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.asset;

import org.eclipse.dataspaceconnector.spi.system.Feature;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

/**
 * Resolves a {@link DataAddress} that is associated with an Asset.
 */
@FunctionalInterface
@Feature(DataAddressResolver.FEATURE)
public interface DataAddressResolver {
    String FEATURE = "edc:asset:dataaddress-resolver";

    /**
     * Resolves a {@link DataAddress} for a given {@code Asset}. A {@code DataAddress} can be understood as a pointer into
     * a storage system like a database or a document store.
     *
     * @param assetId The {@code assetId} for which the data pointer should be fetched.
     * @return A DataAddress
     * @throws IllegalArgumentException if no corresponding {@code DataAddress} was found for a certain asset
     */
    DataAddress resolveForAsset(String assetId);
}
