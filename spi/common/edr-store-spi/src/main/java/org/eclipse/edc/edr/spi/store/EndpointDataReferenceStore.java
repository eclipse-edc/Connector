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

import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.List;

/**
 * Stores and retrieves {@link DataAddress} and {@link EndpointDataReferenceEntry}.
 * in the underlying storage/cache. Implementors of {@link EndpointDataReferenceStore}
 * can decide to split the storage in two parts by using {@link EndpointDataReferenceCache} for caching
 * the actual EDRs in a secured environment and {@link EndpointDataReferenceEntryIndex} for storing the associated metadata.
 */
@ExtensionPoint
public interface EndpointDataReferenceStore {

    /**
     * Return a {@link DataAddress} associated with the transferProcessId in input
     *
     * @param transferProcessId The transferProcessId
     * @return The result containing the {@link DataAddress}
     */
    StoreResult<DataAddress> resolveByTransferProcess(String transferProcessId);

    /**
     * Returns all the EDR entries in the store that are covered by a given {@link QuerySpec}.
     *
     * @param querySpec The {@link QuerySpec}
     * @return success with the matched {@link EndpointDataReferenceEntry}s, failure if some error happens
     */
    StoreResult<List<EndpointDataReferenceEntry>> query(QuerySpec querySpec);

    /**
     * Deletes an {@link EndpointDataReferenceEntry} by the transfer process id.
     *
     * @param transferProcessId The id of the transfer process
     * @return success with the deleted {@link EndpointDataReferenceEntry}, failure if some error happens
     */
    StoreResult<EndpointDataReferenceEntry> delete(String transferProcessId);

    /**
     * Saves an {@link EndpointDataReferenceEntry} and the associated {@link DataAddress} .
     *
     * @param entry       The {@link EndpointDataReferenceEntry} to store
     * @param dataAddress The {@link DataAddress} to store
     * @return success if saved correctly, failure otherwise
     */
    StoreResult<Void> save(EndpointDataReferenceEntry entry, DataAddress dataAddress);
}
