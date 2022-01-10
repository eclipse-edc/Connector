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
 *
 */
package org.eclipse.dataspaceconnector.spi.contract.offer.store;

import org.eclipse.dataspaceconnector.spi.system.Feature;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Persists {@link ContractDefinition}s.
 */
@Feature(ContractDefinitionStore.FEATURE)
public interface ContractDefinitionStore {

    String FEATURE = "edc:core:contract:contractdefinition:store";

    /**
     * Returns all the definitions in the store.
     */
    @NotNull
    Collection<ContractDefinition> findAll();

    /**
     * Persists the definitions.
     */
    void save(Collection<ContractDefinition> definitions);

    /**
     * Persists the definition.
     */
    void save(ContractDefinition definition);

    /**
     * Updates the definitions.
     */
    void update(ContractDefinition definition);

    /**
     * Deletes the definition with the given id.
     */
    void delete(String id);

    /**
     * Signals the store should reload its internal cache if updates were may. If the implementation does not implement caching, this method will do nothing.
     */
    void reload();

}
