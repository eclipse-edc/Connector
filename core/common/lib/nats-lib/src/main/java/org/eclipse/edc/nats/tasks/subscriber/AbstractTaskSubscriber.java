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

package org.eclipse.edc.nats.tasks.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import org.eclipse.edc.controlplane.tasks.ProcessTaskPayload;
import org.eclipse.edc.controlplane.tasks.Task;
import org.eclipse.edc.controlplane.tasks.TaskService;
import org.eclipse.edc.nats.subscriber.NatsSubscriber;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class AbstractTaskSubscriber<P extends ProcessTaskPayload> extends NatsSubscriber {

    protected final Class<P> target;
    protected TaskService taskService;
    protected TransactionContext transactionContext;
    protected Supplier<ObjectMapper> mapperSupplier;
    protected Clock clock;
    protected int maxRetries = 3;

    protected AbstractTaskSubscriber(Class<P> target) {
        this.target = target;
    }


    protected abstract StatusResult<Void> handlePayload(P payload);

    @Override
    protected StatusResult<Void> handleMessage(Message message) {
        try {
            var task = mapperSupplier.get().readValue(message.getData(), Task.class);
            if (target.isAssignableFrom(task.getPayload().getClass())) {
                return transactionContext.execute(() -> handleTask(task));
            } else {
                return StatusResult.failure(ResponseStatus.FATAL_ERROR, "Invalid task payload type");
            }
        } catch (Exception e) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private @NotNull StatusResult<Void> handleTask(Task task) {
        var persistedTask = taskService.findById(task.getId());
        if (persistedTask == null) {
            return StatusResult.failure(ResponseStatus.ERROR_RETRY, "Task not found: " + task.getId());
        }
        var result = handlePayload((P) task.getPayload());
        if (result.succeeded() || result.fatalError()) {
            taskService.delete(task.getId());
        } else {
            if (persistedTask.getRetryCount() >= maxRetries) {
                monitor.severe("Task " + persistedTask.getId() + " reached max retry count of " + maxRetries + ". Dropping task. Last error: " + result.getFailureDetail());
                taskService.delete(persistedTask.getId());
                return StatusResult.success();
            }
            //increment retry count
            taskService.update(task.toBuilder().retryCount(task.getRetryCount() + 1).at(clock.millis()).build());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public abstract static class Builder<T extends AbstractTaskSubscriber<P>, B extends Builder<T, B, P>, P extends ProcessTaskPayload> extends NatsSubscriber.Builder<T, Builder<T, B, P>> {
        protected Builder(T subscriber) {
            super(subscriber);
        }

        public B taskService(TaskService taskService) {
            subscriber.taskService = taskService;
            return (B) self();
        }

        public B transactionContext(TransactionContext transactionContext) {
            subscriber.transactionContext = transactionContext;
            return (B) self();
        }

        public B mapperSupplier(Supplier<ObjectMapper> mapperSupplier) {
            subscriber.mapperSupplier = mapperSupplier;
            return (B) self();
        }

        public B clock(Clock clock) {
            subscriber.clock = clock;
            return (B) self();
        }

        public B maxRetries(int maxRetries) {
            subscriber.maxRetries = maxRetries;
            return (B) self();
        }

        @Override
        public T build() {
            Objects.requireNonNull(subscriber.mapperSupplier, "mapperSupplier must be set");
            Objects.requireNonNull(subscriber.taskService, "taskService must be set");
            Objects.requireNonNull(subscriber.transactionContext, "transactionContext must be set");
            Objects.requireNonNull(subscriber.clock, "clock must be set");
            return super.build();
        }
    }
}
