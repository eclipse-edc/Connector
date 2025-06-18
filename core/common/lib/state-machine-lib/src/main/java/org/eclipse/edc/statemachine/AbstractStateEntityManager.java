/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.statemachine;

import org.eclipse.edc.spi.entity.StateEntityManager;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.StateEntityStore;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.retry.WaitStrategy;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessFactory;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.Objects;

/**
 * Abstraction that provides a common ground for state machine manager implementation.
 *
 * @param <E> the entity type.
 * @param <S> the store type.
 */
public abstract class AbstractStateEntityManager<E extends StatefulEntity<E>, S extends StateEntityStore<E>> implements StateEntityManager {

    protected Monitor monitor;
    protected int batchSize = StateMachineConfiguration.DEFAULT_BATCH_SIZE;
    protected WaitStrategy waitStrategy = () -> StateMachineConfiguration.DEFAULT_ITERATION_WAIT;
    protected ExecutorInstrumentation executorInstrumentation = ExecutorInstrumentation.noop();
    protected Telemetry telemetry = new Telemetry();
    protected EntityRetryProcessConfiguration entityRetryProcessConfiguration = defaultEntityRetryProcessConfiguration();
    protected EntityRetryProcessFactory entityRetryProcessFactory;
    protected StateMachineManager stateMachineManager;
    protected Clock clock = Clock.systemUTC();
    protected S store;

    @Override
    public void start() {
        var stateMachineManagerBuilder = StateMachineManager.Builder
                .newInstance(getClass().getSimpleName(), monitor, executorInstrumentation, waitStrategy);
        stateMachineManager = configureStateMachineManager(stateMachineManagerBuilder).build();

        stateMachineManager.start();
    }

    @Override
    public void stop() {
        if (stateMachineManager != null) {
            stateMachineManager.stop();
        }
    }

    /**
     * configures the State Machine Manager builder
     *
     * @param builder the builder.
     * @return the builder.
     */
    protected abstract StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder);

    @NotNull
    private EntityRetryProcessConfiguration defaultEntityRetryProcessConfiguration() {
        return new EntityRetryProcessConfiguration(
                StateMachineConfiguration.DEFAULT_SEND_RETRY_LIMIT,
                () -> new ExponentialWaitStrategy(StateMachineConfiguration.DEFAULT_SEND_RETRY_BASE_DELAY)
        );
    }

    protected void update(E entity) {
        store.save(entity);
        monitor.debug(() -> "[%s] %s %s is now in state %s"
                .formatted(this.getClass().getSimpleName(), entity.getClass().getSimpleName(),
                        entity.getId(), entity.stateAsString()));
    }

    protected void breakLease(E entity) {
        store.save(entity);
    }

    public abstract static class Builder<E extends StatefulEntity<E>, S extends StateEntityStore<E>, M extends AbstractStateEntityManager<E, S>, B extends Builder<E, S, M, B>> {

        protected final M manager;

        protected Builder(M manager) {
            this.manager = manager;
        }

        public abstract B self();

        public B monitor(Monitor monitor) {
            manager.monitor = monitor;
            return self();
        }

        public B batchSize(int batchSize) {
            manager.batchSize = batchSize;
            return self();
        }

        public B waitStrategy(WaitStrategy waitStrategy) {
            manager.waitStrategy = waitStrategy;
            return self();
        }

        public B clock(Clock clock) {
            manager.clock = clock;
            return self();
        }

        public B telemetry(Telemetry telemetry) {
            manager.telemetry = telemetry;
            return self();
        }

        public B executorInstrumentation(ExecutorInstrumentation executorInstrumentation) {
            manager.executorInstrumentation = executorInstrumentation;
            return self();
        }

        public B entityRetryProcessConfiguration(EntityRetryProcessConfiguration entityRetryProcessConfiguration) {
            manager.entityRetryProcessConfiguration = entityRetryProcessConfiguration;
            return self();
        }

        public B store(S store) {
            manager.store = store;
            return self();
        }

        public M build() {
            Objects.requireNonNull(manager.store, "store");
            Objects.requireNonNull(manager.monitor, "monitor");

            manager.entityRetryProcessFactory = new EntityRetryProcessFactory(manager.monitor, manager.clock, manager.entityRetryProcessConfiguration);

            return manager;
        }
    }

}
