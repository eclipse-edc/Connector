/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.statemachine;

import org.eclipse.edc.statemachine.retry.TestEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessorImplTest {

    @Test
    void shouldReturnTheProcessedCount() {
        var entity = TestEntity.Builder.newInstance().id("id").build();
        var processor = ProcessorImpl.Builder.newInstance(() -> List.of(entity))
                .process(e -> true)
                .build();

        var count = processor.process();

        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldNotCountUnprocessedEntities() {
        var entity = TestEntity.Builder.newInstance().id("id").build();
        var processor = ProcessorImpl.Builder.newInstance(() -> List.of(entity))
                .process(e -> false)
                .build();

        var count = processor.process();

        assertThat(count).isEqualTo(0);
    }

    @Test
    void shouldExecuteGuard_whenItsPredicateMatches() {
        var entity = TestEntity.Builder.newInstance().id("id").build();
        Function<TestEntity, Boolean> process = mock();
        Function<TestEntity, Boolean> guardProcess = mock();
        when(guardProcess.apply(any())).thenReturn(true);
        var processor = ProcessorImpl.Builder.newInstance(() -> List.of(entity))
                .guard(e -> true, guardProcess)
                .process(process)
                .build();

        var count = processor.process();

        assertThat(count).isEqualTo(1);
        verify(guardProcess).apply(entity);
        verifyNoInteractions(process);
    }

    @Test
    void shouldExecuteDefaultProcessor_whenGuardPredicateDoesNotMatch() {
        var entity = TestEntity.Builder.newInstance().id("id").build();
        Function<TestEntity, Boolean> process = mock();
        Function<TestEntity, Boolean> guardProcess = mock();
        when(process.apply(any())).thenReturn(true);
        var processor = ProcessorImpl.Builder.newInstance(() -> List.of(entity))
                .guard(e -> false, guardProcess)
                .process(process)
                .build();

        var count = processor.process();

        assertThat(count).isEqualTo(1);
        verify(process).apply(entity);
        verifyNoInteractions(guardProcess);
    }

    @Test
    void shouldExecuteOnNotProcessed_whenEntityProcessed() {
        var entity = TestEntity.Builder.newInstance().id("id").build();
        Consumer<TestEntity> onNotProcessed = mock();
        var processor = ProcessorImpl.Builder.newInstance(() -> List.of(entity))
                .process(e -> false)
                .onNotProcessed(onNotProcessed)
                .build();

        processor.process();

        verify(onNotProcessed).accept(entity);
    }

    @Test
    void shouldNotExecuteOnNotProcessed_whenEntityProcessed() {
        var entity = TestEntity.Builder.newInstance().id("id").build();
        Consumer<TestEntity> onNotProcessed = mock();
        var processor = ProcessorImpl.Builder.newInstance(() -> List.of(entity))
                .process(e -> true)
                .onNotProcessed(onNotProcessed)
                .build();

        processor.process();

        verifyNoInteractions(onNotProcessed);
    }
}
