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

import io.nats.client.Message;
import org.eclipse.edc.controlplane.tasks.ProcessTaskPayload;
import org.eclipse.edc.controlplane.tasks.Task;
import org.eclipse.edc.controlplane.tasks.TaskService;
import org.eclipse.edc.nats.subscriber.NatsSubscriber;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import tools.jackson.databind.ObjectMapper;

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
        Task task;
        try {
            task = mapperSupplier.get().readValue(message.getData(), Task.class);
        } catch (Exception e) {
            // a message that cannot be read will never be: it is not retried
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, "Invalid task message: " + e.getMessage());
        }
        if (task.getPayload() == null || !target.isAssignableFrom(task.getPayload().getClass())) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, "Invalid task payload type");
        }

        try {
            return transactionContext.execute(() -> handleTask(message, task));
        } catch (Exception e) {
            // e.g. the database is not reachable: the task is retried with a delay, up to the retry limit
            var deliveredCount = deliveredCount(message);
            if (deliveredCount >= retryLimit) {
                monitor.severe("Task %s failed after %d deliveries, giving up".formatted(task.getId(), deliveredCount), e);
                return StatusResult.failure(ResponseStatus.FATAL_ERROR, e.getMessage());
            }
            monitor.warning("Task %s failed, it will be retried: %s".formatted(task.getId(), e.getMessage()));
            return StatusResult.failure(ResponseStatus.ERROR_RETRY, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private @NotNull StatusResult<Void> handleTask(Message message, Task task) {
        var persistedTask = taskService.findById(task.getId());
        if (persistedTask == null) {
            // The task is published within the producer's transaction, before it commits, so a fast delivery can
            // arrive before the task is visible in the store. Retry until it becomes visible, bounded by the NATS
            // delivery count (which exists even though the task does not) so a task that never commits cannot be
            // redelivered forever.
            if (message.metaData().deliveredCount() >= maxRetries) {
                monitor.severe("Task " + task.getId() + " not found after " + maxRetries + " deliveries. Dropping message.");
                return StatusResult.success();
            }
            return StatusResult.failure(ResponseStatus.ERROR_RETRY, "Task not found: " + task.getId());
        }

        var result = handlePayload((P) task.getPayload());
        if (result.succeeded() || result.fatalError()) {
            taskService.delete(task.getId());
        }
        // On a retryable (ERROR_RETRY) failure the message is nak'd and redelivered. The retry/give-up decision is
        // owned by the processor (bounded by edc.<entity>.send.retry.limit), which terminates the entity on
        // exhaustion; the subscriber deliberately does not cap business retries here.
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
