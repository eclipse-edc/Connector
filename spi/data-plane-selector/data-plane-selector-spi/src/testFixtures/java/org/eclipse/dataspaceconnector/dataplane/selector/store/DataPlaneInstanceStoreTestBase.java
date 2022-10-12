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

package org.eclipse.dataspaceconnector.dataplane.selector.store;

import org.eclipse.dataspaceconnector.dataplane.selector.TestDataPlaneInstance;
import org.eclipse.dataspaceconnector.dataplane.selector.TestFunctions;
import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstance;
import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstanceImpl;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;


public abstract class DataPlaneInstanceStoreTestBase {


    @Test
    void save() {
        var inst = TestFunctions.createInstance("test-id");
        getStore().save(inst);
        assertThat(getStore().getAll()).usingRecursiveFieldByFieldElementComparator().containsExactly(inst);
    }

    @Test
    void save_whenExists_shouldUpsert() {
        var inst = TestFunctions.createInstance("test-id");
        getStore().save(inst);

        var inst2 = DataPlaneInstanceImpl.Builder.newInstance()
                .id("test-id")
                .url("http://somewhere.other:9876/api/v2") //different URL
                .build();

        getStore().save(inst2);

        assertThat(getStore().getAll()).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(inst2);
    }

    @Test
    void saveAll() {
        var allInstances = IntStream.range(0, 10).mapToObj(i -> TestFunctions.createInstance("test-id" + i)).collect(Collectors.toList());
        getStore().saveAll(allInstances);
        assertThat(getStore().getAll())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(allInstances.toArray(new DataPlaneInstance[]{}));
    }

    @Test
    void save_shouldReturnCustomInstance() {
        var custom = TestFunctions.createCustomInstance("test-id", "name");

        getStore().save(custom);

        var customInstance = getStore().findById(custom.getId());


        assertThat(customInstance)
                .isInstanceOf(TestDataPlaneInstance.class)
                .usingRecursiveComparison()
                .isEqualTo(custom);
    }

    @Test
    void findById() {
        var inst = TestFunctions.createInstance("test-id");
        getStore().save(inst);

        assertThat(getStore().findById("test-id")).usingRecursiveComparison().isEqualTo(inst);
    }

    @Test
    void findById_notExists() {
        assertThat(getStore().findById("not-exist")).isNull();
    }


    protected abstract DataPlaneInstanceStore getStore();
}
