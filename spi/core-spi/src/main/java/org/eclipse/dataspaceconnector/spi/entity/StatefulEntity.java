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
 *       Microsoft Corporation - added fields
 *
 */

package org.eclipse.dataspaceconnector.spi.entity;


import org.eclipse.dataspaceconnector.spi.telemetry.TraceCarrier;

import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Base class for state machine persistent entities.
 *
 * @param <T> implementation type ({@link StatefulEntity} sub-class). Used to define {@link #copy()} method.
 */
public abstract class StatefulEntity<T extends StatefulEntity<T>> implements TraceCarrier {

    protected String id;
    protected long createdTimestamp;
    protected int state;
    protected int stateCount;
    protected long stateTimestamp;
    protected Map<String, String> traceContext = new HashMap<>();
    protected String errorDetail;
    protected Clock clock;

    protected StatefulEntity() {
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public int getState() {
        return state;
    }

    public int getStateCount() {
        return stateCount;
    }

    public long getStateTimestamp() {
        return stateTimestamp;
    }

    @Override
    public Map<String, String> getTraceContext() {
        return Collections.unmodifiableMap(traceContext);
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

    /**
     * Sets the state timestamp to the clock time.
     *
     * @see Builder#clock(Clock)
     */
    public void updateStateTimestamp() {
        stateTimestamp = clock.millis();
    }

    protected void transitionTo(int targetState) {
        stateCount = state == targetState ? stateCount + 1 : 1;
        state = targetState;
        updateStateTimestamp();
    }

    public String getId() {
        return id;
    }

    public abstract T copy();

    /**
     * Base Builder class for derived classes.
     *
     * @param <T> type being built ({@link StatefulEntity} sub-class)
     * @param <B> derived Builder ({@link Builder} sub-class)
     * @see <a href="http://egalluzzo.blogspot.com/2010/06/using-inheritance-with-fluent.html">Using inheritance with fluent interfaces (blog post)</a> for a background on the use of generic types.
     */
    protected abstract static class Builder<T extends StatefulEntity<T>, B extends Builder<T, B>> {

        public abstract B self();

        protected final T entity;

        protected Builder(T entity) {
            this.entity = entity;
        }

        public B id(String id) {
            entity.id = id;
            return self();
        }


        public B clock(Clock clock) {
            entity.clock = clock;
            return self();
        }

        public B createdTimestamp(long value) {
            entity.createdTimestamp = value;
            return self();
        }

        public B state(int value) {
            entity.state = value;
            return self();
        }

        public B stateCount(int value) {
            entity.stateCount = value;
            return self();
        }

        public B stateTimestamp(long value) {
            entity.stateTimestamp = value;
            return self();
        }

        public B errorDetail(String errorDetail) {
            entity.errorDetail = errorDetail;
            return self();
        }

        public B traceContext(Map<String, String> traceContext) {
            entity.traceContext = traceContext;
            return self();
        }

        protected T build() {
            Objects.requireNonNull(entity.id, "id");
            entity.clock = Objects.requireNonNullElse(entity.clock, Clock.systemUTC());
            if (entity.stateTimestamp == 0) {
                entity.stateTimestamp = entity.clock.millis();
            }
            return entity;
        }
    }

    protected <B extends Builder<T, B>> T copy(Builder<T, B> builder) {
        return builder
                .id(id)
                .createdTimestamp(createdTimestamp)
                .state(state)
                .stateCount(stateCount)
                .stateTimestamp(stateTimestamp)
                .traceContext(traceContext)
                .errorDetail(errorDetail)
                .clock(clock)
                .build();
    }
}
