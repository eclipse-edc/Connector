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

package org.eclipse.dataspaceconnector.contract.memory;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.contract.ContractStore;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryContractStore implements ContractStore {

    private final ConcurrentMap<String, Contract> contractsById = new ConcurrentHashMap<>();

    @Override
    public Iterable<Contract> getAll() {
        return Collections.unmodifiableCollection(contractsById.values());
    }

    @Override
    public @Nullable Contract getById(@NotNull String id) {
        return contractsById.get(Objects.requireNonNull(id));
    }

    @Override
    public void save(@NotNull Contract contract) {
        Objects.requireNonNull(contract);

        if (contractsById.containsKey(contract.getId())) {
            throw new EdcException(String.format("Cannot store contract. Another contract with the same id is already registered. (id: %s)", contract.getId()));
        }

        contractsById.put(contract.getId(), contract);
    }
}
