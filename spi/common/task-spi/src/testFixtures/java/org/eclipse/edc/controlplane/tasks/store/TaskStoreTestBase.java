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

package org.eclipse.edc.controlplane.tasks.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.controlplane.tasks.ProcessTaskPayload;
import org.eclipse.edc.controlplane.tasks.Task;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public abstract class TaskStoreTestBase {

    protected abstract TaskStore getStore();


    private TestPayload createTestPayload(String processId) {
        return createTestPayload(processId, "transfer.prepare", "transfer.process");
    }

    private TestPayload createTestPayload(String processId, String name, String group) {
        return TestPayload.Builder.newInstance()
                .processId(processId)
                .processState(300)
                .processType("CONSUMER")
                .name(name)
                .group(group)
                .build();
    }

    public static class TestPayload extends ProcessTaskPayload {

        private String group;
        private String name;

        private TestPayload() {
        }

        @JsonProperty("name")
        @Override
        public String name() {
            return name;
        }

        @JsonProperty("group")
        @Override
        public String group() {
            return group;
        }

        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder extends ProcessTaskPayload.Builder<TestPayload, TestPayload.Builder> {

            private Builder() {
                super(new TestPayload());
            }

            @JsonCreator
            public static Builder newInstance() {
                return new TestPayload.Builder();
            }

            public Builder name(String name) {
                task.name = name;
                return this;
            }

            public Builder group(String group) {
                task.group = group;
                return this;
            }

            @Override
            public Builder self() {
                return this;
            }
        }
    }

    @Nested
    class FetchForUpdate {
        @Test
        void fetchForUpdate() {
            var payload1 = createTestPayload("process-1");
            var task1 = Task.Builder.newInstance()
                    .at(System.currentTimeMillis())
                    .payload(payload1).build();

            var payload2 = createTestPayload("process-2");
            var task2 = Task.Builder.newInstance()
                    .at(System.currentTimeMillis())
                    .payload(payload2).build();

            getStore().create(task1);
            getStore().create(task2);

            var results = getStore().fetchForUpdate(QuerySpec.none());

            assertThat(results).hasSize(2)
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactlyInAnyOrder(task1, task2);
        }

        @Test
        void fetchForUpdate_with_OrderBy() {
            var payload1 = createTestPayload("process-1");
            var task1 = Task.Builder.newInstance()
                    .at(2000L)
                    .payload(payload1).build();

            var payload2 = createTestPayload("process-2");
            var task2 = Task.Builder.newInstance()
                    .at(1000L)
                    .payload(payload2).build();

            getStore().create(task1);
            getStore().create(task2);

            var query = QuerySpec.Builder.newInstance().sortField("at")
                    .build();

            var results = getStore().fetchForUpdate(query);

            assertThat(results).hasSize(2)
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(task2, task1);
        }

        @Test
        void fetchForUpdate_filterByGroup() {
            var payload1 = createTestPayload("process-1");
            var task1 = Task.Builder.newInstance()
                    .at(2000L)
                    .payload(payload1).build();

            var payload2 = createTestPayload("process-2");
            var task2 = Task.Builder.newInstance()
                    .at(1000L)
                    .payload(payload2).build();

            var payload3 = createTestPayload("process-2", "transfer.complete", "other-group");

            var task3 = Task.Builder.newInstance()
                    .at(3000L)
                    .payload(payload3).build();

            getStore().create(task1);
            getStore().create(task2);
            getStore().create(task3);

            var query = QuerySpec.Builder.newInstance()
                    .filter(Criterion.criterion("group", "=", payload2.group()))
                    .sortField("at")
                    .build();

            var results = getStore().fetchForUpdate(query);

            assertThat(results).hasSize(2)
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(task2, task1);
        }

        @Test
        void fetchForUpdate_filterByByName() {
            var payload1 = createTestPayload("process-1");
            var task1 = Task.Builder.newInstance()
                    .at(2000L)
                    .payload(payload1).build();

            var payload2 = createTestPayload("process-2");
            var task2 = Task.Builder.newInstance()
                    .at(1000L)
                    .payload(payload2).build();

            var payload3 = createTestPayload("process-2", "transfer.complete", "transfer.process");

            var task3 = Task.Builder.newInstance()
                    .at(3000L)
                    .payload(payload3).build();

            getStore().create(task1);
            getStore().create(task2);
            getStore().create(task3);

            var query = QuerySpec.Builder.newInstance()
                    .filter(Criterion.criterion("name", "=", payload1.name()))
                    .sortField("at")
                    .build();

            var results = getStore().fetchForUpdate(query);

            assertThat(results).hasSize(2)
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(task2, task1);
        }

        @Test
        void fetchForUpdate_empty() {
            var results = getStore().fetchForUpdate(QuerySpec.none());

            assertThat(results).isEmpty();
        }
    }

    @Nested
    class Update {
        @Test
        void update_shouldModifyExistingTask() {
            var payload = createTestPayload("process-1");
            var task = Task.Builder.newInstance()
                    .at(System.currentTimeMillis())
                    .payload(payload).build();
            getStore().create(task);


            var updatedTask = Task.Builder.newInstance()
                    .at(System.currentTimeMillis())
                    .id(task.getId())
                    .payload(payload)
                    .retryCount(task.getRetryCount() + 1)
                    .build();
            getStore().update(updatedTask);

            var retrieved = getStore().findById(task.getId());
            assertThat(retrieved).usingRecursiveComparison().isEqualTo(updatedTask);
        }

        @Test
        void update_when_notFound() {
            var payload = createTestPayload("process-1");
            var updatedTask = Task.Builder.newInstance()
                    .at(System.currentTimeMillis())
                    .payload(payload).build();


            var result = getStore().update(updatedTask);
            assertThat(result).isFailed();

        }
    }

    @Nested
    class Delete {
        @Test
        void delete_shouldRemoveTask() {
            var payload = createTestPayload("process-1");
            var task = Task.Builder.newInstance()
                    .at(System.currentTimeMillis())
                    .payload(payload).build();
            getStore().create(task);

            var result = getStore().delete(task.getId());

            assertThat(result).isSucceeded();
            assertThat(getStore().findById(task.getId())).isNull();
        }

        @Test
        void delete_when_notFound() {
            var result = getStore().delete("nonexistent-id");
            assertThat(result).isFailed();
        }

    }

    @Nested
    class FindById {
        @Test
        void findById_shouldReturnTaskWhenPresent() {
            var payload = createTestPayload("process-1");
            var task = Task.Builder.newInstance()
                    .at(System.currentTimeMillis())
                    .payload(payload).build();
            getStore().create(task);

            var retrieved = getStore().findById(task.getId());

            assertThat(retrieved).usingRecursiveComparison().isEqualTo(task);
        }

        @Test
        void findById_shouldReturnNullWhenNotFound() {
            var retrieved = getStore().findById("nonexistent-id");

            assertThat(retrieved).isNull();
        }
    }

    @Nested
    class Create {
        @Test
        void create_shouldStoreTask() {
            var payload = createTestPayload("process-1");
            var task = Task.Builder.newInstance()
                    .at(System.currentTimeMillis())
                    .payload(payload).build();

            var result = getStore().create(task);

            assertThat(result).isSucceeded();

            var retrieved = getStore().findById(task.getId());
            assertThat(retrieved).usingRecursiveComparison().isEqualTo(task);
        }

        @Test
        void create_shouldGenerateUniqueIds() {
            var payload1 = createTestPayload("process-1");
            var task1 = Task.Builder.newInstance()
                    .at(System.currentTimeMillis())
                    .payload(payload1).build();

            var payload2 = createTestPayload("process-2");
            var task2 = Task.Builder.newInstance()
                    .at(System.currentTimeMillis())
                    .payload(payload2).build();

            getStore().create(task1);
            getStore().create(task2);

            assertThat(task1.getId()).isNotEqualTo(task2.getId());
            assertThat(getStore().findById(task1.getId())).usingRecursiveComparison().isEqualTo(task1);
            assertThat(getStore().findById(task2.getId())).usingRecursiveComparison().isEqualTo(task2);
        }

        @Test
        void create_shouldAllowMultipleTasksForSameProcess() {
            var payload1 = createTestPayload("process-1");
            var task1 = Task.Builder.newInstance()
                    .at(System.currentTimeMillis())
                    .payload(payload1).build();

            var payload2 = createTestPayload("process-1");
            var task2 = Task.Builder.newInstance()
                    .at(System.currentTimeMillis())
                    .payload(payload2).build();

            getStore().create(task1);
            getStore().create(task2);

            var results = getStore().fetchForUpdate(QuerySpec.none());
            assertThat(results).hasSize(2);
        }

    }
}
