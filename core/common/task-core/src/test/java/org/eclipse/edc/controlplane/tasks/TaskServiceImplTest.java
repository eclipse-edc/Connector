/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.controlplane.tasks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.controlplane.tasks.store.TaskStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskServiceImplTest {

    private final TaskStore taskStore = mock();
    private final TaskObservableImpl taskObservable = new TaskObservableImpl();
    private final TaskListener taskListener = mock();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private TaskServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TaskServiceImpl(taskStore, taskObservable, transactionContext);
        taskObservable.registerListener(taskListener);
    }

    @Test
    void create_shouldPersistTask() {
        var payload = TestPayload.Builder.newInstance()
                .processId("process-1")
                .processState(300)
                .processType("CONSUMER")
                .build();
        var task = Task.Builder.newInstance()
                .at(System.currentTimeMillis())
                .payload(payload)
                .build();
        when(taskStore.create(any())).thenReturn(StoreResult.success());

        var result = service.create(task);
        assertThat(result).isSucceeded();

        verify(taskStore).create(task);
    }

    @Test
    void create_shouldInvokeTaskListeners() {
        var payload = TestPayload.Builder.newInstance()
                .processId("process-1")
                .processState(300)
                .processType("CONSUMER")
                .build();
        var task = Task.Builder.newInstance()
                .at(System.currentTimeMillis())
                .payload(payload)
                .build();

        when(taskStore.create(any())).thenReturn(StoreResult.success());

        var result = service.create(task);
        assertThat(result).isSucceeded();

        verify(taskStore).create(task);
        verify(taskListener).created(any());

    }

    @Test
    void update() {
        var payload = TestPayload.Builder.newInstance()
                .processId("process-1")
                .processState(300)
                .processType("CONSUMER")
                .build();
        when(taskStore.update(any())).thenReturn(StoreResult.success());
        var task = Task.Builder.newInstance()
                .at(System.currentTimeMillis())
                .payload(payload)
                .build();


        var result = service.update(task);
        assertThat(result).isSucceeded();

        verify(taskStore).update(task);

    }

    @Test
    void fetchLatestTask_shouldRetrieveTasks() {
        var payload1 = TestPayload.Builder.newInstance()
                .processId("process-1")
                .processState(300)
                .processType("CONSUMER")
                .build();
        var task1 = Task.Builder.newInstance()
                .at(System.currentTimeMillis())
                .payload(payload1)
                .build();

        var payload2 = TestPayload.Builder.newInstance()
                .processId("process-2")
                .processState(300)
                .processType("CONSUMER")
                .build();
        var task2 = Task.Builder.newInstance()
                .at(System.currentTimeMillis())
                .payload(payload2)
                .build();

        when(taskStore.fetchForUpdate(any(QuerySpec.class)))
                .thenReturn(List.of(task1, task2));

        var results = service.fetchLatestTask(QuerySpec.none());

        assertThat(results).hasSize(2);
        verify(taskStore).fetchForUpdate(any(QuerySpec.class));
    }

    @Test
    void fetchLatestTask_shouldReturnEmptyWhenNoTasksAvailable() {
        when(taskStore.fetchForUpdate(any(QuerySpec.class)))
                .thenReturn(List.of());

        var results = service.fetchLatestTask(QuerySpec.none());

        assertThat(results).isEmpty();
        verify(taskStore).fetchForUpdate(any(QuerySpec.class));
    }

    @Test
    void delete_shouldRemoveTask() {
        var taskId = "task-123";
        when(taskStore.delete(taskId)).thenReturn(StoreResult.success());

        var result = service.delete(taskId);
        assertThat(result).isSucceeded();

        verify(taskStore).delete(taskId);
    }

    @Test
    void findById_shouldReturnTask() {
        var taskId = "task-123";
        var payload = TestPayload.Builder.newInstance()
                .processId("process-1")
                .processState(300)
                .processType("CONSUMER")
                .build();
        var task = Task.Builder.newInstance()
                .id(taskId)
                .at(System.currentTimeMillis())
                .payload(payload)
                .build();

        when(taskStore.findById(taskId)).thenReturn((Task) task);

        var result = service.findById(taskId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(taskId);
        verify(taskStore).findById(taskId);
    }

    @Test
    void findById_shouldReturnNullWhenTaskNotFound() {
        when(taskStore.findById("nonexistent-id")).thenReturn(null);

        var result = service.findById("nonexistent-id");

        assertThat(result).isNull();
        verify(taskStore).findById("nonexistent-id");
    }

    @Test
    void create_shouldAllowMultipleTasksForSameProcess() {
        var payload1 = TestPayload.Builder.newInstance()
                .processId("process-1")
                .processState(300)
                .processType("CONSUMER")
                .build();
        var task1 = Task.Builder.newInstance()
                .at(System.currentTimeMillis())
                .payload(payload1)
                .build();

        var payload2 = TestPayload.Builder.newInstance()
                .processId("process-1")
                .processState(300)
                .processType("CONSUMER")
                .build();
        var task2 = Task.Builder.newInstance()
                .at(System.currentTimeMillis())
                .payload(payload2)
                .build();

        when(taskStore.fetchForUpdate(any(QuerySpec.class)))
                .thenReturn(List.of(task1, task2));

        var results = service.fetchLatestTask(QuerySpec.none());

        assertThat(results).hasSize(2);
        verify(taskStore).fetchForUpdate(any(QuerySpec.class));
    }


    /**
     * Test payload implementation for TaskService tests
     */
    public static class TestPayload extends ProcessTaskPayload {

        private String group = "test.group";
        private String name = "test.payload";

        private TestPayload() {
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String group() {
            return group;
        }

        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder extends ProcessTaskPayload.Builder<TestPayload, Builder> {

            private Builder() {
                super(new TestPayload());
            }

            @JsonCreator
            public static Builder newInstance() {
                return new TestPayload.Builder();
            }

            public Builder group(String group) {
                task.group = group;
                return this;
            }

            public Builder name(String name) {
                task.name = name;
                return this;
            }

            @Override
            public Builder self() {
                return this;
            }
        }
    }
}
