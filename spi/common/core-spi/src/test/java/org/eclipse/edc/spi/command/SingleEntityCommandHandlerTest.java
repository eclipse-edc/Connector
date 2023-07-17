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

package org.eclipse.edc.spi.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.persistence.StateEntityStore;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.command.CommandFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.command.CommandFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SingleEntityCommandHandlerTest {

    private final StateEntityStore<TestEntity> store = mock();
    private final TestCommandHandler handler = new TestCommandHandler(store);

    @Test
    void shouldGetAndLeaseEntityModifyAndPersist() {
        var entity = TestEntity.Builder.newInstance().clock(incrementingClock()).build();
        var startingUpdatedAt = entity.getUpdatedAt();
        when(store.findByIdAndLease(any())).thenReturn(StoreResult.success(entity));

        var result = handler.handle(new TestCommand("id", true));

        assertThat(result).isSucceeded();
        verify(store).findByIdAndLease("id");
        var captor = ArgumentCaptor.forClass(TestEntity.class);
        verify(store).save(captor.capture());
        var persistedEntity = captor.getValue();
        assertThat(persistedEntity.getUpdatedAt()).isGreaterThan(startingUpdatedAt);
        assertThat(entity.getErrorDetail()).contains("postActions");
    }

    @Test
    void shouldFail_whenEntityIsNotFound() {
        when(store.findByIdAndLease(any())).thenReturn(StoreResult.notFound("entity does not exist"));

        var result = handler.handle(new TestCommand("id", true));

        assertThat(result).isFailed().extracting(CommandFailure::getReason).isEqualTo(NOT_FOUND);
    }

    @Test
    void shouldFail_whenEntityCannotBeLeased() {
        when(store.findByIdAndLease(any())).thenReturn(StoreResult.alreadyLeased("entity is already leased"));

        var result = handler.handle(new TestCommand("id", true));

        assertThat(result).isFailed().extracting(CommandFailure::getReason).isEqualTo(CONFLICT);
    }

    @Test
    void shouldFailAndBreakLease_whenCommandDoesNotModifyTheEntity() {
        var entity = TestEntity.Builder.newInstance().clock(incrementingClock()).build();
        when(store.findByIdAndLease(any())).thenReturn(StoreResult.success(entity));

        var result = handler.handle(new TestCommand("id", false));

        assertThat(result).isFailed().extracting(CommandFailure::getReason).isEqualTo(CONFLICT);
        verify(store).save(entity);
        assertThat(entity.getErrorDetail()).isNull();
    }

    @NotNull
    private static Clock incrementingClock() {
        var clock = mock(Clock.class);
        var timestamp = new AtomicLong(1L);
        when(clock.millis()).thenAnswer(i -> timestamp.incrementAndGet());
        return clock;
    }

    private static class TestCommandHandler extends SingleEntityCommandHandler<TestCommand, TestEntity> {

        TestCommandHandler(StateEntityStore<TestEntity> store) {
            super(store);
        }

        @Override
        public Class<TestCommand> getType() {
            return null;
        }

        @Override
        protected boolean modify(TestEntity entity, TestCommand command) {
            return command.willModify;
        }

        @Override
        public void postActions(TestEntity entity, TestCommand command) {
            entity.setErrorDetail("just to being able to verify thet postActions has been called.");
        }
    }

    private static class TestCommand extends SingleEntityCommand {

        private final boolean willModify;

        TestCommand(String entityId, boolean willModify) {
            super(entityId);
            this.willModify = willModify;
        }
    }

    private static class TestEntity extends StatefulEntity<TestEntity> {
        @Override
        public TestEntity copy() {
            return this;
        }

        @Override
        public String stateAsString() {
            return "STATE";
        }

        public static class Builder extends StatefulEntity.Builder<TestEntity, Builder> {

            private Builder(TestEntity entity) {
                super(entity);
            }

            @JsonCreator
            public static Builder newInstance() {
                return new Builder(new TestEntity());
            }

            @Override
            public Builder self() {
                return this;
            }

            @Override
            protected TestEntity build() {
                return super.build();
            }
        }
    }
}
