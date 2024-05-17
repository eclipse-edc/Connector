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

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.spi.result.StoreFailure;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;


public abstract class DataPlaneInstanceStoreTestBase {

    @Test
    void save() {
        var inst = createInstanceBuilder("test-id").build();
        getStore().create(inst);
        assertThat(getStore().getAll()).usingRecursiveFieldByFieldElementComparator().containsExactly(inst);
    }

    @Test
    void save_withAllowedTransferTypes() {
        var inst = createInstanceBuilder("test-id").allowedTransferType("transfer-type").build();
        getStore().create(inst);
        assertThat(getStore().getAll()).usingRecursiveFieldByFieldElementComparator().containsExactly(inst);
    }

    @Test
    void save_whenExists_shouldNotUpsert() {
        var inst = createInstanceBuilder("test-id").build();
        getStore().create(inst);


        var inst2 = DataPlaneInstance.Builder.newInstance()
                .id("test-id")
                .url("http://somewhere.other:9876/api/v2") //different URL
                .build();

        var result = getStore().create(inst2);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().getReason()).isEqualTo(ALREADY_EXISTS);

        assertThat(getStore().getAll()).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(inst);
    }

    @Test
    void update_whenExists_shouldUpdate() {
        var inst = createInstanceBuilder("test-id").build();
        getStore().create(inst);


        var inst2 = DataPlaneInstance.Builder.newInstance()
                .id("test-id")
                .url("http://somewhere.other:9876/api/v2") //different URL
                .build();

        var result = getStore().update(inst2);

        assertThat(result.succeeded()).isTrue();
        assertThat(getStore().getAll()).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(inst2);
    }

    @Test
    void save_shouldReturnCustomInstance() {
        var custom = createInstanceWithProperty("test-id", "name");

        getStore().create(custom);

        var customInstance = getStore().findById(custom.getId());

        assertThat(customInstance)
                .isInstanceOf(DataPlaneInstance.class)
                .usingRecursiveComparison()
                .isEqualTo(custom);
    }

    @Test
    void findById() {
        var inst = createInstanceBuilder("test-id").build();
        getStore().create(inst);

        assertThat(getStore().findById("test-id")).usingRecursiveComparison().isEqualTo(inst);
    }

    @Test
    void findById_notExists() {
        assertThat(getStore().findById("not-exist")).isNull();
    }

    @Test
    void getAll() {
        var doc1 = createInstanceWithProperty("test-id", "name");
        var doc2 = createInstanceWithProperty("test-id-2", "name");

        var store = getStore();

        store.create(doc1);
        store.create(doc2);

        var foundItems = store.getAll();

        assertThat(foundItems).isNotNull().hasSize(2);
    }

    @Nested
    class DeleteById {

        @Test
        void shouldDeleteDataPlaneInstanceById() {
            var id = UUID.randomUUID().toString();
            var instance = createInstanceBuilder(id).build();
            getStore().create(instance);

            var result = getStore().deleteById(id);

            assertThat(result).isSucceeded().usingRecursiveComparison().isEqualTo(instance);
        }

        @Test
        void shouldFail_whenInstanceDoesNotExist() {
            var randomId = UUID.randomUUID().toString();

            var result = getStore().deleteById(randomId);

            assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(NOT_FOUND);
        }

    }

    protected abstract DataPlaneInstanceStore getStore();


    private DataPlaneInstance createInstanceWithProperty(String id, String name) {
        return createInstanceBuilder(id)
                .property("name", name)
                .build();
    }

    private DataPlaneInstance.Builder createInstanceBuilder(String id) {
        return DataPlaneInstance.Builder.newInstance()
                .id(id)
                .url("http://somewhere.com:1234/api/v1");
    }

}
