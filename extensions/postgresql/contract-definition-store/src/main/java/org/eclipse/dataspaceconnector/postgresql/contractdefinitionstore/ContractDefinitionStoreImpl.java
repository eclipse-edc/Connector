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

package org.eclipse.dataspaceconnector.postgresql.contractdefinitionstore;

import org.eclipse.dataspaceconnector.clients.postgresql.asset.Repository;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;

public class ContractDefinitionStoreImpl implements ContractDefinitionStore {

    private final Repository repository;

    public ContractDefinitionStoreImpl(@NotNull Repository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public @NotNull Collection<ContractDefinition> findAll() {
        try {
            return repository.queryAllContractDefinitions();
        } catch (SQLException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public void save(Collection<ContractDefinition> definitions) {
        try {
            repository.create(definitions);
        } catch (SQLException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public void save(ContractDefinition definition) {
        try {
            repository.create(definition);
        } catch (SQLException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public void update(ContractDefinition definition) {
        try {
            repository.update(definition);
        } catch (SQLException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public void delete(ContractDefinition contractDefinition) {
        try {
            repository.delete(contractDefinition);
        } catch (SQLException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public void reload() {
        // do nothing
    }
}
