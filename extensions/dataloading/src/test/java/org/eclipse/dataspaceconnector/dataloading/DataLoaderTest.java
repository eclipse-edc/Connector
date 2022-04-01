/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.dataloading;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;


class DataLoaderTest {

    private static final String INDEX_VALIDATION_MESSAGE = "index must be > 0!";
    private static final String DESCRIPTION_VALIDATION_MESSAGE = "Description cannot be null!";
    private final TestEntitySink sink = mock(TestEntitySink.class);
    private DataLoader<TestEntity> dataLoader;

    @BeforeEach
    void setUp() {
        dataLoader = DataLoader.Builder.<TestEntity>newInstance()
                .sink(sink)
                .andPredicate(testEntity -> testEntity.getDescription() != null ? Result.success(testEntity) : Result.failure(DESCRIPTION_VALIDATION_MESSAGE))
                .andPredicate(testEntity -> testEntity.getIndex() > 0 ? Result.success(testEntity) : Result.failure(INDEX_VALIDATION_MESSAGE))
                .build();
    }

    @Test
    void insert() {
        var te = new TestEntity("Test Desc", 3);

        dataLoader.insert(te);

        verify(sink).accept(te);
        verifyNoMoreInteractions(sink);
    }

    @Test
    void insert_oneValidationFails() {
        var te = new TestEntity("Test Desc", -3);

        assertThatThrownBy(() -> dataLoader.insert(te)).isInstanceOf(ValidationException.class)
                .hasMessage(INDEX_VALIDATION_MESSAGE);
        verifyNoMoreInteractions(sink);
    }

    @Test
    void insert_multipleValidationsFail() {
        var te = new TestEntity(null, -3);

        assertThatThrownBy(() -> dataLoader.insert(te)).isInstanceOf(ValidationException.class)
                .hasMessageContaining(INDEX_VALIDATION_MESSAGE)
                .hasMessageContaining(DESCRIPTION_VALIDATION_MESSAGE);
        verifyNoInteractions(sink);
    }

    @Test
    void insertAll() {
        var items = IntStream.range(1, 10).mapToObj(i -> new TestEntity("Test Item " + i, i)).collect(Collectors.toList());

        dataLoader.insertAll(items);

        verify(sink, times(9)).accept(any(TestEntity.class));
        verifyNoMoreInteractions(sink);
    }

    @Test
    void insertAll_oneItemFails() {
        var items = IntStream.range(1, 10).mapToObj(i -> new TestEntity("Test Item " + i, i)).collect(Collectors.toList());
        items.add(new TestEntity("Invalid entity", -9));

        assertThatThrownBy(() -> dataLoader.insertAll(items)).isInstanceOf(ValidationException.class)
                .hasMessageContaining(INDEX_VALIDATION_MESSAGE);
        verifyNoInteractions(sink);
    }

    @Test
    void insertAll_oneItemFailsWithMultipleValidationErrors() {
        var items = IntStream.range(1, 10).mapToObj(i -> new TestEntity("Test Item " + i, i)).collect(Collectors.toList());
        items.add(new TestEntity("Invalid entity", -9));
        items.add(new TestEntity(null, -9));

        assertThatThrownBy(() -> dataLoader.insertAll(items)).isInstanceOf(ValidationException.class)
                .hasMessageContaining(INDEX_VALIDATION_MESSAGE)
                .hasMessageContaining(DESCRIPTION_VALIDATION_MESSAGE);
        verifyNoInteractions(sink);
    }

    @Test
    void insertAll_multipleItemsFail() {
        var items = IntStream.range(1, 10).mapToObj(i -> new TestEntity(null, i)).collect(Collectors.toList());

        assertThatThrownBy(() -> dataLoader.insertAll(items)).isInstanceOf(ValidationException.class)
                .hasMessageContaining(DESCRIPTION_VALIDATION_MESSAGE);
        verifyNoInteractions(sink);
    }
}