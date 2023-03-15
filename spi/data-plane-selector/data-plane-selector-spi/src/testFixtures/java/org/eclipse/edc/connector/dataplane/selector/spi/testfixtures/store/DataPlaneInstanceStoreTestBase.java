/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.selector.spi.testfixtures.store;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.spi.testfixtures.TestFunctions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;


public abstract class DataPlaneInstanceStoreTestBase {


    @Test
    void save() {
        var inst = TestFunctions.createInstance("test-id");
        getStore().create(inst);
        Assertions.assertThat(getStore().getAll()).usingRecursiveFieldByFieldElementComparator().containsExactly(inst);
    }

    @Test
    void save_whenExists_shouldNotUpsert() {
        var inst = TestFunctions.createInstance("test-id");
        getStore().create(inst);


        var inst2 = DataPlaneInstance.Builder.newInstance()
                .id("test-id")
                .url("http://somewhere.other:9876/api/v2") //different URL
                .build();

        var result = getStore().create(inst2);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().getReason()).isEqualTo(ALREADY_EXISTS);

        Assertions.assertThat(getStore().getAll()).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(inst);
    }

    @Test
    void update_whenExists_shouldUpdate() {
        var inst = TestFunctions.createInstance("test-id");
        getStore().create(inst);


        var inst2 = DataPlaneInstance.Builder.newInstance()
                .id("test-id")
                .url("http://somewhere.other:9876/api/v2") //different URL
                .build();

        var result = getStore().update(inst2);

        assertThat(result.succeeded()).isTrue();

        Assertions.assertThat(getStore().getAll()).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(inst2);
    }


    @Test
    void save_shouldReturnCustomInstance() {
        var custom = TestFunctions.createCustomInstance("test-id", "name");

        getStore().create(custom);

        var customInstance = getStore().findById(custom.getId());


        Assertions.assertThat(customInstance)
                .isInstanceOf(DataPlaneInstance.class)
                .usingRecursiveComparison()
                .isEqualTo(custom);
    }

    @Test
    void findById() {
        var inst = TestFunctions.createInstance("test-id");
        getStore().create(inst);

        Assertions.assertThat(getStore().findById("test-id")).usingRecursiveComparison().isEqualTo(inst);
    }

    @Test
    void findById_notExists() {
        Assertions.assertThat(getStore().findById("not-exist")).isNull();
    }

    @Test
    void getAll() {
        var doc1 = TestFunctions.createCustomInstance("test-id", "name");
        var doc2 = TestFunctions.createCustomInstance("test-id-2", "name");

        var store = getStore();

        store.create(doc1);
        store.create(doc2);

        var foundItems = store.getAll();

        assertThat(foundItems).isNotNull().hasSize(2);
    }


    protected abstract DataPlaneInstanceStore getStore();
}
