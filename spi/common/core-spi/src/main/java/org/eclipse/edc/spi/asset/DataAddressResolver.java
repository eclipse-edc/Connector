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

package org.eclipse.edc.spi.asset;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * Resolves a {@link DataAddress} that is associated with an Asset.
 */
@FunctionalInterface
@ExtensionPoint
public interface DataAddressResolver {
    /**
     * Resolves a {@link DataAddress} for a given {@code Asset}. A {@code DataAddress} can be understood as a pointer into
     * a storage system like a database or a document store.
     *
     * @param assetId The {@code assetId} for which the data pointer should be fetched.
     * @return A DataAddress, null if not found
     */
    DataAddress resolveForAsset(String assetId);
}
