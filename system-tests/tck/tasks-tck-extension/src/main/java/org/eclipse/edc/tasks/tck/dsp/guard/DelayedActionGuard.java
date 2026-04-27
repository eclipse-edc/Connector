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

package org.eclipse.edc.tasks.tck.dsp.guard;

import org.eclipse.edc.spi.entity.PendingGuard;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.persistence.StateEntityStore;
import org.eclipse.edc.tasks.tck.dsp.recorder.Step;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * A guard that performs actions on a stateful entity.
 * <p>
 * Note this implementation is not safe to use in a clustered environment since transitions are not performed in the context of
 * a command handler.
 */
public class DelayedActionGuard<T extends StatefulEntity<T>> implements PendingGuard<T> {
    private final Function<T, Step<T>> actionProvider;
    private final DelayQueue<GuardDelay> queue;
    private final AtomicBoolean active = new AtomicBoolean();
    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    private final StateEntityStore<T> store;

    public DelayedActionGuard(Function<T, Step<T>> actionProvider,
                              StateEntityStore<T> store) {
        this.actionProvider = actionProvider;
        this.store = store;
        queue = new DelayQueue<>();
    }

    public void start() {
        active.set(true);
        executor.submit(() -> {
            while (active.get()) {
                try {
                    var entry = queue.poll(10, MILLISECONDS);
                    if (entry != null) {
                        entry.action.accept(entry.entity);
                        entry.entity.setPending(false);
                        store.save(entry.entity);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.interrupted();
                    break;
                }
            }
        });
    }

    public void stop() {
        active.set(false);
    }

    @Override
    public boolean test(T entity) {
        var actionGuard = actionProvider.apply(entity);
        if (actionGuard instanceof Step.ContinueStep) {
            return false;
        } else if (actionGuard instanceof Step.SkipStep) {
            return true;
        } else if (actionGuard instanceof Step.InterceptStep<T> intercept) {
            queue.put(new GuardDelay(entity, intercept));
            return true;
        }
        return false;
    }

    protected class GuardDelay implements Delayed {
        private final long start;
        T entity;
        Step.InterceptStep<T> action;

        GuardDelay(T entity, Step.InterceptStep<T> action) {
            this.entity = entity;
            this.action = action;
            start = System.currentTimeMillis();
        }

        @Override
        public int compareTo(@NotNull Delayed delayed) {
            var millis = getDelay(MILLISECONDS) - delayed.getDelay(MILLISECONDS);
            millis = Math.min(millis, 1);
            millis = Math.max(millis, -1);
            return (int) millis;
        }

        @Override
        public long getDelay(@NotNull TimeUnit timeUnit) {
            return timeUnit.convert(500 - (System.currentTimeMillis() - start), MILLISECONDS);
        }

    }


}
