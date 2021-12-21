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

package org.eclipse.dataspaceconnector.clients.postgresql.asset.operation;

import org.assertj.core.api.Assertions;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.Repository;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@IntegrationTest
public class ContractDefinitionDeleteOperationTest extends AbstractOperationTest {

    private Repository repository;

    @BeforeEach
    public void setup() {
        repository = getRepository();
    }

    @Test
    public void testContractDefinitionDeletion() throws SQLException {
        Criterion criterion = new Criterion("hello", "=", "world");
        AssetSelectorExpression selectorExpression = AssetSelectorExpression.Builder.newInstance()
                .criteria(Collections.singletonList(criterion))
                .build();
        Policy contractPolicy = Policy.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();
        Policy accessPolicy = Policy.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();

        ContractDefinition contractDefinition = ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .selectorExpression(selectorExpression)
                .contractPolicy(contractPolicy)
                .accessPolicy(accessPolicy)
                .build();

        repository.create(contractDefinition);
        repository.delete(contractDefinition);

        List<ContractDefinition> storedDefinitions = repository.queryAllContractDefinitions();

        Assertions.assertThat(storedDefinitions).size().isEqualTo(0);
    }
}
