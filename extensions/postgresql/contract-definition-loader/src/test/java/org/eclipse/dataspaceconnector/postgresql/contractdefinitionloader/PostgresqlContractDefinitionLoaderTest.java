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

package org.eclipse.dataspaceconnector.postgresql.contractdefinitionloader;

import org.eclipse.dataspaceconnector.clients.postgresql.asset.Repository;
import org.eclipse.dataspaceconnector.dataloading.ContractDefinitionLoader;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import java.sql.SQLException;

import static org.mockito.Mockito.times;

public class PostgresqlContractDefinitionLoaderTest {
    private ContractDefinitionLoader contractDefinitionLoader;

    // mocks
    private Repository repository;

    @BeforeEach
    public void setup() {
        repository = Mockito.mock(Repository.class);
        contractDefinitionLoader = new PostgresContractDefinitionLoader(repository);
    }

    @Test
    public void testAccept() throws SQLException {
        ContractDefinition contractDefinition = ContractDefinition.Builder.newInstance()
                .contractPolicy(Policy.Builder.newInstance().build())
                .accessPolicy(Policy.Builder.newInstance().build())
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .build();

        contractDefinitionLoader.accept(contractDefinition);

        Mockito.verify(repository, times(1))
                .create(contractDefinition);
    }
}
