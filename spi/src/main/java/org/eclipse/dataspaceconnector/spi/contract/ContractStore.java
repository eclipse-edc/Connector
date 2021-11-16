/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.contract;

import org.eclipse.dataspaceconnector.spi.types.domain.contract.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Store to persist {@link Contract} objects
 */
public interface ContractStore {

    String FEATURE = "edc:contract:store";

    /**
     * Get all stored contracts.
     *
     * @return iterable of contracts
     */
    Iterable<Contract> getAll();

    /**
     * Gets a contract by its identifier.
     *
     * @param id of the contract
     * @return contract or null if not found
     */
    @Nullable
    Contract getById(@NotNull String id);

    /**
     * Save a contract. <br>
     * Throws an {@link org.eclipse.dataspaceconnector.spi.EdcException} if a contract
     * with the same id is already registered.
     *
     * @param contract to save
     */
    void save(@NotNull Contract contract);
}
