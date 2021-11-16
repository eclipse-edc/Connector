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

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.Contract;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryContractStoreTest {

    @Test
    public void testExceptionOnDuplicatedId() {
        var store = new InMemoryContractStore();
        var contract1 = createContract("1");
        var contract2 = createContract("1");

        store.save(contract1);
        Assertions.assertThrows(EdcException.class, () -> store.save(contract2));
    }

    @Test
    public void testStoreAndRetrieveById() {
        var store = new InMemoryContractStore();
        var contract = createContract("123");

        store.save(contract);
        var loadedContract = store.getById(contract.getId());

        Assertions.assertEquals(loadedContract, contract);
    }

    @Test
    public void testStoreManyAndRetrieveAll() {
        var store = new InMemoryContractStore();
        var contract = createContract("123");
        var contract2 = createContract("456");
        var contract3 = createContract("789");

        store.save(contract);
        store.save(contract2);
        store.save(contract3);

        var loadedContracts = store.getAll();

        assertThat(loadedContracts).containsExactlyInAnyOrder(contract, contract2, contract3);
    }

    private Contract createContract(String id) {
        Contract.Builder contractBuilder = Contract.Builder.newInstance()
                .id(id)
                .providerId("https://provider.com")
                .consumerId("https://consumer.com")
                .contractSigningDate(999)
                .contractStartDate(1000)
                .contractEndDate(1001)
                .policy(Policy.Builder.newInstance().build())
                .assets(Collections.emptyList());

        return contractBuilder.build();
    }

}
