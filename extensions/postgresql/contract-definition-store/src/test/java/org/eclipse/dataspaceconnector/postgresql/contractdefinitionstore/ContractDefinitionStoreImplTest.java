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
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.mockito.Mockito.times;

public class ContractDefinitionStoreImplTest {

    private ContractDefinitionStore contractDefinitionStore;

    // mocks
    private Repository repository;

    @BeforeEach
    public void setup() {
        repository = Mockito.mock(Repository.class);
        contractDefinitionStore = new ContractDefinitionStoreImpl(repository);
    }

    @Test
    public void testDelete() throws SQLException {
        ContractDefinition contractDefinition = createDefinition();

        contractDefinitionStore.delete(contractDefinition);

        Mockito.verify(repository, times(1))
                .delete(contractDefinition);
    }

    @Test
    public void testFindAll() throws SQLException {
        contractDefinitionStore.findAll();

        Mockito.verify(repository, times(1))
                .queryAllContractDefinitions();
    }

    @Test
    public void testSave() throws SQLException {
        ContractDefinition contractDefinition = createDefinition();

        contractDefinitionStore.save(contractDefinition);

        Mockito.verify(repository, times(1))
                .create(contractDefinition);
    }

    @Test
    public void testUpdate() throws SQLException {
        ContractDefinition contractDefinition = createDefinition();

        contractDefinitionStore.update(contractDefinition);

        Mockito.verify(repository, times(1))
                .update(contractDefinition);
    }

    @Test
    public void testSaveMany() throws SQLException {
        Collection<ContractDefinition> contractDefinitions = Arrays.asList(
                createDefinition(), createDefinition());

        contractDefinitionStore.save(contractDefinitions);

        Mockito.verify(repository, times(1))
                .create(contractDefinitions);
    }

    private ContractDefinition createDefinition() {
        return ContractDefinition.Builder.newInstance().id(UUID.randomUUID().toString())
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .contractPolicy(Policy.Builder.newInstance().build())
                .accessPolicy(Policy.Builder.newInstance().build())
                .build();
    }
}
