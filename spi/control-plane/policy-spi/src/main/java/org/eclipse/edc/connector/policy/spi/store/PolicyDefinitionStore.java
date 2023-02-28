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

package org.eclipse.edc.connector.policy.spi.store;

import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Persists {@link Policy}.
 */
@ExtensionPoint
public interface PolicyDefinitionStore {

    /**
     * Finds the policy by id.
     *
     * @param policyId id of the policy.
     * @return {@link Policy} or null if not found.
     * @throws EdcPersistenceException if something goes wrong.
     */
    PolicyDefinition findById(String policyId);

    /**
     * Find stream of policies in the store based on query spec.
     *
     * @param spec query specification.
     * @return A {@link Stream} of {@link Policy}. Might be empty, never null.
     * @throws EdcPersistenceException if something goes wrong.
     */
    Stream<PolicyDefinition> findAll(QuerySpec spec);

    /**
     * Persists the policy.
     *
     * @param policy to be saved.
     * @throws EdcPersistenceException if something goes wrong.
     */
    void save(PolicyDefinition policy);

    /**
     * Updates the policy.
     *
     * @param policyId ID of the Policy to be updated
     * @param policy to be updated.
     * @throws EdcPersistenceException if something goes wrong.
     */
    PolicyDefinition update(String policyId, PolicyDefinition policy);

    /**
     * Deletes a policy for the given id.
     *
     * @param policyId id of the policy to be removed.
     * @return Deleted {@link Policy} or null if policy not found.
     * @throws EdcPersistenceException if something goes wrong.
     */
    @Nullable
    PolicyDefinition deleteById(String policyId);

    /**
     * If the store implementation supports caching, this method triggers a cache-reload.
     */
    default void reload() {

    }
}
