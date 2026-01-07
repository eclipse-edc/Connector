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

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceStates;
import org.eclipse.edc.connector.dataplane.selector.spi.manager.DataPlaneAvailabilityChecker;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.response.StatusResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceStates.AVAILABLE;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceStates.REGISTERED;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceStates.UNAVAILABLE;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DataPlaneSelectorManagerImplTest {

    private final DataPlaneInstanceStore store = mock();
    private final DataPlaneAvailabilityChecker availabilityChecker = mock();
    private final Duration checkPeriod = Duration.of(10, SECONDS);
    private final Instant now = Instant.now();
    private final Clock clock = Clock.fixed(now, ZoneId.systemDefault());
    private final DataPlaneSelectorManagerImpl manager = DataPlaneSelectorManagerImpl.Builder.newInstance()
            .monitor(mock())
            .store(store)
            .availabilityChecker(availabilityChecker)
            .checkPeriod(checkPeriod)
            .clock(clock)
            .build();

    @Nested
    class Registered {

        @Test
        void shouldTransitionToAvailable_whenDataPlaneIsAvailable() {
            var instance = DataPlaneInstance.Builder.newInstance().state(REGISTERED.code()).url("http://any").build();
            when(store.nextNotLeased(anyInt(), stateIs(REGISTERED))).thenReturn(List.of(instance)).thenReturn(emptyList());
            when(availabilityChecker.checkAvailability(any())).thenReturn(StatusResult.success());

            manager.start();

            await().untilAsserted(() -> {
                verify(store).save(argThat(it -> it.getState() == AVAILABLE.code()));
            });
        }

        @Test
        void shouldTransitionToUnavailable_whenDataPlaneIsNotAvailable() {
            var instance = DataPlaneInstance.Builder.newInstance().state(REGISTERED.code()).url("http://any").build();
            when(store.nextNotLeased(anyInt(), stateIs(REGISTERED))).thenReturn(List.of(instance)).thenReturn(emptyList());
            when(availabilityChecker.checkAvailability(any())).thenReturn(StatusResult.failure(FATAL_ERROR));

            manager.start();

            await().untilAsserted(() -> {
                verify(store).save(argThat(it -> it.getState() == UNAVAILABLE.code()));
            });
        }

    }

    @Nested
    class Available {

        @Test
        void shouldRemainAvailable_whenDataPlaneIsAvailable() {
            var updatedAt = now.minus(checkPeriod).minus(1, MILLIS);
            var instance = DataPlaneInstance.Builder.newInstance().state(AVAILABLE.code()).url("http://any")
                    .updatedAt(updatedAt.toEpochMilli()).build();
            when(store.nextNotLeased(anyInt(), stateIs(AVAILABLE))).thenReturn(List.of(instance)).thenReturn(emptyList());
            when(availabilityChecker.checkAvailability(any())).thenReturn(StatusResult.success());

            manager.start();

            await().untilAsserted(() -> {
                verify(store).save(argThat(it -> it.getState() == AVAILABLE.code()));
            });
        }

        @Test
        void shouldTransitionToUnavailable_whenDataPlaneIsNotAvailable() {
            var updatedAt = now.minus(checkPeriod).minus(1, MILLIS);
            var instance = DataPlaneInstance.Builder.newInstance().state(AVAILABLE.code()).url("http://any")
                    .updatedAt(updatedAt.toEpochMilli()).build();
            when(store.nextNotLeased(anyInt(), stateIs(AVAILABLE))).thenReturn(List.of(instance)).thenReturn(emptyList());
            when(availabilityChecker.checkAvailability(any())).thenReturn(StatusResult.failure(FATAL_ERROR));

            manager.start();

            await().untilAsserted(() -> {
                verify(store).save(argThat(it -> it.getState() == UNAVAILABLE.code()));
            });
        }

        @Test
        void shouldNotCheckAvailability_whenCheckPeriodIsLowerThanConfiguredOne() {
            var updatedAt = now.minus(checkPeriod).plus(1, MILLIS);
            var instance = DataPlaneInstance.Builder.newInstance().state(AVAILABLE.code()).url("http://any")
                    .updatedAt(updatedAt.toEpochMilli()).build();
            when(store.nextNotLeased(anyInt(), stateIs(AVAILABLE))).thenReturn(List.of(instance)).thenReturn(emptyList());
            when(availabilityChecker.checkAvailability(any())).thenReturn(StatusResult.success());

            manager.start();

            await().untilAsserted(() -> {
                verifyNoInteractions(availabilityChecker);
                verify(store, atLeast(2)).nextNotLeased(anyInt(), stateIs(AVAILABLE));
            });
        }
    }

    @Nested
    class Unavailable {

        @Test
        void shouldRemainUnavailable_whenDataPlaneIsUnavailable() {
            var updatedAt = now.minus(checkPeriod).minus(1, MILLIS);
            var instance = DataPlaneInstance.Builder.newInstance().state(UNAVAILABLE.code()).url("http://any")
                    .updatedAt(updatedAt.toEpochMilli()).build();
            when(store.nextNotLeased(anyInt(), stateIs(UNAVAILABLE))).thenReturn(List.of(instance)).thenReturn(emptyList());
            when(availabilityChecker.checkAvailability(any())).thenReturn(StatusResult.failure(FATAL_ERROR));

            manager.start();

            await().untilAsserted(() -> {
                verify(store).save(argThat(it -> it.getState() == UNAVAILABLE.code()));
            });
        }

        @Test
        void shouldTransitionToAvailable_whenDataPlaneIsAvailable() {
            var updatedAt = now.minus(checkPeriod).minus(1, MILLIS);
            var instance = DataPlaneInstance.Builder.newInstance().state(UNAVAILABLE.code()).url("http://any")
                    .updatedAt(updatedAt.toEpochMilli()).build();
            when(store.nextNotLeased(anyInt(), stateIs(UNAVAILABLE))).thenReturn(List.of(instance)).thenReturn(emptyList());
            when(availabilityChecker.checkAvailability(any())).thenReturn(StatusResult.success());

            manager.start();

            await().untilAsserted(() -> {
                verify(store).save(argThat(it -> it.getState() == AVAILABLE.code()));
            });
        }

        @Test
        void shouldNotCheckAvailability_whenCheckPeriodIsLowerThanConfiguredOne() {
            var updatedAt = now.minus(checkPeriod).plus(1, MILLIS);
            var instance = DataPlaneInstance.Builder.newInstance().state(UNAVAILABLE.code()).url("http://any")
                    .updatedAt(updatedAt.toEpochMilli()).build();
            when(store.nextNotLeased(anyInt(), stateIs(UNAVAILABLE))).thenReturn(List.of(instance)).thenReturn(emptyList());
            when(availabilityChecker.checkAvailability(any())).thenReturn(StatusResult.success());

            manager.start();

            await().untilAsserted(() -> {
                verifyNoInteractions(availabilityChecker);
                verify(store, atLeast(2)).nextNotLeased(anyInt(), stateIs(UNAVAILABLE));
            });
        }
    }

    private Criterion[] stateIs(DataPlaneInstanceStates state) {
        return aryEq(new Criterion[]{ hasState(state.code()) });
    }
}
