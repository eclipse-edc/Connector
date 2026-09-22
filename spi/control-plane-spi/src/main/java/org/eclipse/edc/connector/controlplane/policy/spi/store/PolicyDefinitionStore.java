/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.policy.spi.store;

import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.stream.Stream;

/**
 * Persists {@link Policy}. Policy definition IDs are unique only within a participant context: the pair
 * {@code (participantContextId, id)} identifies a policy definition.
 */
@ExtensionPoint
public interface PolicyDefinitionStore {

    String POLICY_NOT_FOUND = "Policy with ID %s could not be found";
    String POLICY_ALREADY_EXISTS = "Policy with ID %s already exists";

    /**
     * Finds the policy by id, scoped to the given participant context. A policy that exists but belongs to a different
     * participant context is not returned.
     *
     * @param participantContextId the id of the participant context that owns the policy.
     * @param policyId             id of the policy.
     * @return {@link PolicyDefinition} if found within the participant context, null otherwise.
     * @throws EdcPersistenceException if something goes wrong.
     */
    PolicyDefinition findById(String participantContextId, String policyId);

    /**
     * Find stream of policies in the store based on query spec.
     *
     * @param spec query specification.
     * @return A {@link Stream} of {@link Policy}. Might be empty, never null.
     * @throws EdcPersistenceException if something goes wrong.
     */
    Stream<PolicyDefinition> findAll(QuerySpec spec);

    /**
     * Persists the policy, if it does not yet exist within the participant context of the policy.
     *
     * @param policy to be saved.
     * @return {@link StoreResult#success()} if it could be stored, {@link StoreResult#alreadyExists(String)} if a policy with the same ID already exists in the same participant context.
     * @throws EdcPersistenceException if something goes wrong.
     */
    StoreResult<PolicyDefinition> create(PolicyDefinition policy);

    /**
     * Updates the policy, identified by its ID and its participant context ID.
     *
     * @param policy to be updated.
     * @return {@link StoreResult#success()} if it could be updated, {@link StoreResult#notFound(String)} if a policy with the same ID was not found in the participant context.
     * @throws EdcPersistenceException if any exception occurs during Query Execution e.g., SQLException.
     */
    StoreResult<PolicyDefinition> update(PolicyDefinition policy);

    /**
     * Deletes a policy for the given id within the given participant context.
     *
     * @param participantContextId the id of the participant context that owns the policy.
     * @param policyId             id of the policy to be removed.
     * @return {@link StoreResult#success()} if was deleted, {@link StoreResult#notFound(String)} if a policy with the same ID was not found in the participant context.
     * @throws EdcPersistenceException if something goes wrong.
     */
    StoreResult<PolicyDefinition> delete(String participantContextId, String policyId);

    /**
     * If the store implementation supports caching, this method triggers a cache-reload.
     */
    default void reload() {

    }
}
