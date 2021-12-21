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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@IntegrationTest
public class ContractDefinitionCreateOperationTest extends AbstractOperationTest {

    private Repository repository;

    @BeforeEach
    public void setup() {
        repository = getRepository();
    }

    @Test
    public void testContractDefinitionSingleCreation() throws SQLException {
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

        List<ContractDefinition> storedDefinitions = repository.queryAllContractDefinitions();
        ContractDefinition storedDefinition = storedDefinitions.stream()
                .filter(d -> d.getId().equals(contractDefinition.getId()))
                .findFirst()
                .orElse(null);

        Assertions.assertThat(storedDefinition).isNotNull();
        Assertions.assertThat(storedDefinition.getSelectorExpression()).isNotNull();
        Assertions.assertThat(storedDefinition.getSelectorExpression().getCriteria().get(0)).isEqualTo(criterion);
        Assertions.assertThat(storedDefinition.getAccessPolicy()).isNotNull();
        Assertions.assertThat(storedDefinition.getAccessPolicy().getUid()).isEqualTo(accessPolicy.getUid());
        Assertions.assertThat(storedDefinition.getContractPolicy()).isNotNull();
        Assertions.assertThat(storedDefinition.getContractPolicy().getUid()).isEqualTo(contractPolicy.getUid());
    }

    @Test
    public void testContractDefinitionMultipleCreation() throws SQLException {
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

        ContractDefinition contractDefinition2 = ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .selectorExpression(selectorExpression)
                .contractPolicy(contractPolicy)
                .accessPolicy(accessPolicy)
                .build();
        ContractDefinition contractDefinition = ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .selectorExpression(selectorExpression)
                .contractPolicy(contractPolicy)
                .accessPolicy(accessPolicy)
                .build();
        ContractDefinition contractDefinition3 = ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .selectorExpression(selectorExpression)
                .contractPolicy(contractPolicy)
                .accessPolicy(accessPolicy)
                .build();

        repository.create(Arrays.asList(contractDefinition, contractDefinition2, contractDefinition3));

        List<ContractDefinition> storedDefinitions = repository.queryAllContractDefinitions();

        Assertions.assertThat(storedDefinitions).size().isEqualTo(3);
    }
}
