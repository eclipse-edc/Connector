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

import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstance;
import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstanceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.selector.TestFunctions.createInstance;

class DefaultDataPlaneInstanceStoreTest {

    private DefaultDataPlaneInstanceStore store;

    @BeforeEach
    void setup() {
        store = new DefaultDataPlaneInstanceStore();
    }

    @Test
    void save() {
        var inst = createInstance("test-id");
        store.save(inst);
        assertThat(store.getAll()).containsExactly(inst);
    }

    @Test
    void save_whenExists_shouldUpsert() {
        var inst = createInstance("test-id");
        store.save(inst);

        var inst2 = DataPlaneInstanceImpl.Builder.newInstance()
                .id("test-id")
                .url("http://somewhere.other:9876/api/v2") //different URL
                .build();

        store.save(inst2);

        assertThat(store.getAll()).hasSize(1).containsExactly(inst2);
    }

    @Test
    void saveAll() {
        var allInstances = IntStream.range(0, 10).mapToObj(i -> createInstance("test-id" + i)).collect(Collectors.toList());
        store.saveAll(allInstances);
        assertThat(store.getAll()).containsExactlyInAnyOrder(allInstances.toArray(new DataPlaneInstance[]{}));
    }

    @Test
    void findById() {
        var inst = createInstance("test-id");
        store.save(inst);

        assertThat(store.findById("test-id")).isEqualTo(inst);
    }

    @Test
    void findById_notExists() {
        assertThat(store.findById("not-exist")).isNull();
    }

}