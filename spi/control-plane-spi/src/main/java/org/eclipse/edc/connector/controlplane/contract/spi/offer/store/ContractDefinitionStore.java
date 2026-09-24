/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - added method
 *
 */

package org.eclipse.edc.connector.controlplane.contract.spi.offer.store;

import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * Persists {@link ContractDefinition}s. Contract definition IDs are unique only within a participant context: the pair
 * {@code (participantContextId, id)} identifies a contract definition.
 */
@ExtensionPoint
public interface ContractDefinitionStore {


    String CONTRACT_DEFINITION_EXISTS = "Contract Definition with ID %s already exists";
    String CONTRACT_DEFINITION_NOT_FOUND = "Contract Definition with ID %s not found";

    /**
     * Returns all the definitions in the store that are covered by a given {@link QuerySpec}.
     * <p>
     * Note: supplying a sort field that does not exist on the {@link ContractDefinition} may cause some implementations
     * to return an empty Stream, others will return an unsorted Stream, depending on the backing storage
     * implementation.
     */
    @NotNull
    Stream<ContractDefinition> findAll(QuerySpec spec);

    /**
     * Returns the definition with the given id, scoped to the given participant context. A definition that exists but
     * belongs to a different participant context is not returned.
     *
     * @param participantContextId the id of the participant context that owns the definition.
     * @param definitionId         the id.
     * @return the definition with the given id, or null.
     */
    ContractDefinition findById(String participantContextId, String definitionId);

    /**
     * Stores the contract definition if a contract definition with the same ID doesn't already exist within the
     * participant context of the definition.
     *
     * @param definition {@link ContractDefinition} to store.
     * @return {@link StoreResult#success()} if the contract definition was stored, {@link StoreResult#alreadyExists(String)} if a contract
     *         definition with the same ID already exists in the same participant context.
     */
    StoreResult<Void> save(ContractDefinition definition);

    /**
     * Update the contract definition if a contract definition with the same ID exists within the participant context of
     * the definition.
     *
     * @param definition {@link ContractDefinition} to update.
     * @return {@link StoreResult#success()} if the contract definition was updates, {@link StoreResult#notFound(String)} if a contract
     *         definition identified by the ID was not found in the participant context.
     */
    StoreResult<Void> update(ContractDefinition definition);

    /**
     * Deletes the contract definition with the given id within the given participant context.
     *
     * @param participantContextId the id of the participant context that owns the definition.
     * @param id                   A String that represents the {@link ContractDefinition} ID, in most cases this will be a UUID.
     * @return {@link StoreResult#success()}} if the contract definition was deleted, {@link StoreResult#notFound(String)} if the contract definition
     *         was not found in the participant context.
     */
    StoreResult<ContractDefinition> deleteById(String participantContextId, String id);

}
