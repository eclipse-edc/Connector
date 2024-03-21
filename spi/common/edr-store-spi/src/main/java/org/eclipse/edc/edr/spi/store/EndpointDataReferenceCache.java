/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.edr.spi.store;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * Client side Cache for {@link DataAddress} associated to a transfer process in PULL scenario
 */
@ExtensionPoint
public interface EndpointDataReferenceCache {

    /**
     * Resolves an {@link DataAddress} for the transfer process
     *
     * @param transferProcessId The id of the transfer process
     * @return The {@link DataAddress} if found, failure otherwise
     */
    StoreResult<DataAddress> get(String transferProcessId);

    /**
     * Caches a {@link DataAddress} for a transfer process
     *
     * @param transferProcessId The id of the transfer process
     * @param edr               The {@link DataAddress} to cache
     * @return success if cached correctly, failure otherwise
     */
    StoreResult<Void> put(String transferProcessId, DataAddress edr);

    /**
     * Deletes a {@link DataAddress} from the cache
     *
     * @param transferProcessId The id of the transfer process
     * @return success if deleted correctly, failure otherwise
     */
    StoreResult<Void> delete(String transferProcessId);

}
