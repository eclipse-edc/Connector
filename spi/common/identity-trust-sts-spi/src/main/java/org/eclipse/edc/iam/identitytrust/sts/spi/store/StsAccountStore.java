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


import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * Persists and retrieves {@link StsAccount}s.
 */
@ExtensionPoint
public interface StsAccountStore {

    String CLIENT_EXISTS_TEMPLATE = "Client with ID %s already exists";
    String CLIENT_NOT_FOUND_BY_CLIENT_ID_TEMPLATE = "Client with clientID %s not found";
    String CLIENT_NOT_FOUND_BY_ID_TEMPLATE = "Client with id %s not found";

    /**
     * Stores the {@link  StsAccount}
     *
     * @param stsAccount The client
     * @return successful when the client is stored, failure otherwise
     */
    StoreResult<StsAccount> create(StsAccount stsAccount);


    /**
     * Update the {@link StsAccount} if am sts client with the same ID exists.
     *
     * @param stsAccount {@link StsAccount} to update.
     * @return {@link StoreResult#success()} if the sts client was updates, {@link StoreResult#notFound(String)}  if the sts client identified by the ID was not found.
     */
    StoreResult<Void> update(StsAccount stsAccount);

    /**
     * Returns all the sts clients in the store that are covered by a given {@link QuerySpec}.
     * <p>
     * Note: supplying a sort field that does not exist on the {@link StsAccount} may cause some implementations
     * to return an empty Stream, others will return an unsorted Stream, depending on the backing storage
     * implementation.
     */
    @NotNull
    Stream<StsAccount> findAll(QuerySpec spec);

    /**
     * Returns an {@link StsAccount} by its id
     *
     * @param id id of the client
     * @return the client successful if found, failure otherwise
     */
    StoreResult<StsAccount> findById(String id);

    /**
     * Returns an {@link StsAccount} by its clientId
     *
     * @param clientId clientId of the client
     * @return the client successful if found, failure otherwise
     */
    StoreResult<StsAccount> findByClientId(String clientId);

    /**
     * Deletes the sts client with the given id.
     *
     * @param id A String that represents the {@link StsAccount} ID, in most cases this will be a UUID.
     * @return {@link StoreResult#success()}} if the sts client was deleted, {@link StoreResult#notFound(String)}  if the sts client was not found in the store.
     */
    StoreResult<StsAccount> deleteById(String id);
}
