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
package org.eclipse.dataspaceconnector.contractdefinition.store.memory;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression.SELECT_ALL;

class InMemoryContractDefinitionStoreTest {
    private final InMemoryContractDefinitionStore store = new InMemoryContractDefinitionStore();

    @Test
    void verifyStore() {
        var policy = Policy.Builder.newInstance().build();
        var definition1 = ContractDefinition.Builder.newInstance().id("1").accessPolicy(policy).contractPolicy(policy).selectorExpression(SELECT_ALL).build();
        var definition2 = ContractDefinition.Builder.newInstance().id("2").accessPolicy(policy).contractPolicy(policy).selectorExpression(SELECT_ALL).build();

        store.save(definition1);
        assertThat(store.findAll()).contains(definition1);

        store.save(List.of(definition2));
        assertThat(store.findAll()).contains(definition1);

        store.delete(definition1);
        assertThat(store.findAll()).doesNotContain(definition1);
    }
}
