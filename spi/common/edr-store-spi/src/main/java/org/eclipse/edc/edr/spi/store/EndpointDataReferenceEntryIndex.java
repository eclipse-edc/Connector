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
 * Stores and queries metadata {@link EndpointDataReferenceEntry} associated with an EDR ({@link DataAddress})
 */
@ExtensionPoint
public interface EndpointDataReferenceEntryIndex {

    /**
     * Returns all the EDR entries in the store that are covered by a given {@link QuerySpec}.
     *
     * @param querySpec The {@link QuerySpec}
     * @return success with the matched {@link EndpointDataReferenceEntry}s
     */
    StoreResult<List<EndpointDataReferenceEntry>> query(QuerySpec querySpec);

    /**
     * Saves an {@link EndpointDataReferenceEntry}.
     *
     * @param entry The {@link EndpointDataReferenceEntry} to store
     * @return success if saved correctly, failure otherwise
     */
    StoreResult<Void> save(EndpointDataReferenceEntry entry);

    /**
     * Deletes an {@link EndpointDataReferenceEntry} by the transfer process id.
     *
     * @param transferProcessId The id of the transfer process
     * @return success with the deleted {@link EndpointDataReferenceEntry}, failure if some error happens
     */
    StoreResult<EndpointDataReferenceEntry> delete(String transferProcessId);

}
