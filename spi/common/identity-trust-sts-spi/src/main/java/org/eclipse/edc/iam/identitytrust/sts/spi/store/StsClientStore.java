/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.identitytrust.sts.spi.store;


import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsClient;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * Persists and retrieves {@link StsClient}s.
 */
@ExtensionPoint
public interface StsClientStore {

    String CLIENT_EXISTS_TEMPLATE = "Client with ID %s already exists";
    String CLIENT_NOT_FOUND_BY_CLIENT_ID_TEMPLATE = "Client with clientID %s not found";
    String CLIENT_NOT_FOUND_BY_ID_TEMPLATE = "Client with id %s not found";

    /**
     * Stores the {@link  StsClient}
     *
     * @param stsClient The client
     * @return successful when the client is stored, failure otherwise
     */
    StoreResult<StsClient> create(StsClient stsClient);


    /**
     * Update the {@link StsClient} if am sts client with the same ID exists.
     *
     * @param stsClient {@link StsClient} to update.
     * @return {@link StoreResult#success()} if the sts client was updates, {@link StoreResult#notFound(String)}  if the sts client identified by the ID was not found.
     */
    StoreResult<Void> update(StsClient stsClient);

    /**
     * Returns all the sts clients in the store that are covered by a given {@link QuerySpec}.
     * <p>
     * Note: supplying a sort field that does not exist on the {@link StsClient} may cause some implementations
     * to return an empty Stream, others will return an unsorted Stream, depending on the backing storage
     * implementation.
     */
    @NotNull
    Stream<StsClient> findAll(QuerySpec spec);

    /**
     * Returns an {@link StsClient} by its id
     *
     * @param id id of the client
     * @return the client successful if found, failure otherwise
     */
    StoreResult<StsClient> findById(String id);

    /**
     * Returns an {@link StsClient} by its clientId
     *
     * @param clientId clientId of the client
     * @return the client successful if found, failure otherwise
     */
    StoreResult<StsClient> findByClientId(String clientId);

    /**
     * Deletes the sts client with the given id.
     *
     * @param id A String that represents the {@link StsClient} ID, in most cases this will be a UUID.
     * @return {@link StoreResult#success()}} if the sts client was deleted, {@link StoreResult#notFound(String)}  if the sts client was not found in the store.
     */
    StoreResult<StsClient> deleteById(String id);
}
