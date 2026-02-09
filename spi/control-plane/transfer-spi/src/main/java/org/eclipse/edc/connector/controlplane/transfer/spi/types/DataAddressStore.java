/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.spi.types;

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * Represents a store for managing {@link DataAddress} entries associated with {@link TransferProcess} instances.
 * This interface provides methods to store and resolve data addresses.
 */
public interface DataAddressStore {

    /**
     * Stores the provided {@link DataAddress} entry associated with a given {@link TransferProcess}.
     * The operation can succeed or fail, and the result is encapsulated in the returned {@link Result}.
     *
     * @param dataAddress The {@link DataAddress} to be stored. Must not be null.
     * @param transferProcess The {@link TransferProcess} associated with the data address. Must not be null.
     * @return A {@link Result} indicating the success or failure of the store operation.
     *         A successful result will contain no content, while a failure result will contain error details.
     */
    StoreResult<Void> store(DataAddress dataAddress, TransferProcess transferProcess);

    /**
     * Resolves a {@link DataAddress} associated with the given {@link TransferProcess}.
     * The process involves locating the data address within the store based on the provided transfer process.
     *
     * @param transferProcess The {@link TransferProcess} for which the associated {@link DataAddress} needs to be resolved. Must not be null.
     * @return A {@link Result} containing the resolved {@link DataAddress} if the operation is successful.
     *         If the resolution fails, the result will encapsulate error details indicating the reason for failure.
     */
    StoreResult<DataAddress> resolve(TransferProcess transferProcess);

    /**
     * Removes the {@link DataAddress} associated with the specified {@link TransferProcess} from the store.
     * If the operation succeeds, the removed data address is returned.
     * If the operation fails, an appropriate error is encapsulated in the returned result.
     *
     * @param transferProcess The {@link TransferProcess} whose associated {@link DataAddress} is to be removed. Must not be null.
     * @return A {@link StoreResult} containing the removed {@link DataAddress} if the operation is successful.
     *         If the removal fails, the result will encapsulate the corresponding error details.
     */
    StoreResult<DataAddress> remove(TransferProcess transferProcess);

}
