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

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;


public abstract class DataPlaneInstanceStoreTestBase {


    @Test
    void save() {
        var inst = TestFunctions.createInstance("test-id");
        getStore().updateOrCreate(inst);
        Assertions.assertThat(getStore().getAll()).usingRecursiveFieldByFieldElementComparator().containsExactly(inst);
    }

    @Test
    void save_whenExists_shouldUpsert() {
        var inst = TestFunctions.createInstance("test-id");
        getStore().updateOrCreate(inst);

        var inst2 = DataPlaneInstance.Builder.newInstance()
                .id("test-id")
                .url("http://somewhere.other:9876/api/v2") //different URL
                .build();

        getStore().updateOrCreate(inst2);

        Assertions.assertThat(getStore().getAll()).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(inst2);
    }

    @Test
    void saveAll() {
        var allInstances = IntStream.range(0, 10).mapToObj(i -> TestFunctions.createInstance("test-id" + i)).collect(Collectors.toList());
        getStore().updateOrCreateAll(allInstances);
        Assertions.assertThat(getStore().getAll())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(allInstances.toArray(new DataPlaneInstance[]{}));
    }

    @Test
    void save_shouldReturnCustomInstance() {
        var custom = TestFunctions.createCustomInstance("test-id", "name");

        getStore().updateOrCreate(custom);

        var customInstance = getStore().findById(custom.getId());


        Assertions.assertThat(customInstance)
                .isInstanceOf(DataPlaneInstance.class)
                .usingRecursiveComparison()
                .isEqualTo(custom);
    }

    @Test
    void findById() {
        var inst = TestFunctions.createInstance("test-id");
        getStore().updateOrCreate(inst);

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

        store.updateOrCreate(doc1);
        store.updateOrCreate(doc2);

        var foundItems = store.getAll();

        assertThat(foundItems).isNotNull().hasSize(2);
    }


    protected abstract DataPlaneInstanceStore getStore();
}
