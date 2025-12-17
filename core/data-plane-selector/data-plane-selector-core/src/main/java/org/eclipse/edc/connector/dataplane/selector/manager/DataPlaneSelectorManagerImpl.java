/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.selector.manager;

import org.eclipse.edc.connector.dataplane.selector.DataPlaneSelectorManagerConfiguration;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceStates;
import org.eclipse.edc.connector.dataplane.selector.spi.manager.DataPlaneSelectorManager;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.statemachine.AbstractStateEntityManager;
import org.eclipse.edc.statemachine.Processor;
import org.eclipse.edc.statemachine.ProcessorImpl;
import org.eclipse.edc.statemachine.StateMachineManager;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceStates.AVAILABLE;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceStates.REGISTERED;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceStates.UNAVAILABLE;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;

public class DataPlaneSelectorManagerImpl extends AbstractStateEntityManager<DataPlaneInstance, DataPlaneInstanceStore> implements DataPlaneSelectorManager {

    private DataPlaneClientFactory clientFactory;
    private Duration checkPeriod = Duration.ofMinutes(1);

    private DataPlaneSelectorManagerImpl() {
    }

    @Override
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        return builder
                .processor(processEntriesInState(REGISTERED, this::availability))
                .processor(processEntriesInState(AVAILABLE, this::checkAvailability))
                .processor(processEntriesInState(UNAVAILABLE, this::checkAvailability));
    }

    private boolean checkAvailability(DataPlaneInstance instance) {
        if (Duration.between(Instant.ofEpochMilli(instance.getUpdatedAt()), clock.instant()).compareTo(checkPeriod) < 0) {
            return false;
        }

        return availability(instance);
    }

    private boolean availability(DataPlaneInstance instance) {
        var client = clientFactory.createClient(instance);
        var availability = client.checkAvailability();
        if (availability.succeeded()) {
            instance.transitionToAvailable();
        } else {
            monitor.warning("data-plane %s is unavailable: %s".formatted(instance.getId(), availability.getFailureDetail()));
            instance.setErrorDetail(availability.getFailureDetail());
            instance.transitionToUnavailable();
        }
        update(instance);
        return true;
    }

    private Processor processEntriesInState(DataPlaneInstanceStates state, Function<DataPlaneInstance, Boolean> function) {
        var filter = new Criterion[]{ hasState(state.code()) };
        return ProcessorImpl.Builder.newInstance(() -> store.nextNotLeased(batchSize, filter))
                .process(telemetry.contextPropagationMiddleware(function))
                .onNotProcessed(this::breakLease)
                .build();
    }

    public static class Builder
            extends AbstractStateEntityManager.Builder<DataPlaneInstance, DataPlaneInstanceStore, DataPlaneSelectorManagerImpl, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new DataPlaneSelectorManagerImpl());
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder clientFactory(DataPlaneClientFactory clientFactory) {
            manager.clientFactory = clientFactory;
            return this;
        }

        public Builder checkPeriod(Duration checkPeriod) {
            manager.checkPeriod = checkPeriod;
            return this;
        }

        public Builder configuration(DataPlaneSelectorManagerConfiguration configuration) {
            return waitStrategy(configuration.waitStrategy())
                    .batchSize(configuration.batchSize())
                    .checkPeriod(configuration.checkPeriod());
        }
    }
}
