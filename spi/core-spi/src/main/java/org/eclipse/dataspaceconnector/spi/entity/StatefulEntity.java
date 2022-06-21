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
 *
 */

package org.eclipse.dataspaceconnector.spi.entity;

import java.util.Objects;

/**
 * Represents an entity, with id and state.
 * It is abstract since it shouldn't be instantiated directly, but extended.
 */
public abstract class StatefulEntity {

    protected String id;
    protected int state;
    protected int stateCount;
    protected long stateTimestamp;

    /**
     * Returns the id of the entity
     *
     * @return id of the entity
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the current state.
     *
     * @return The current state.
     */
    public int getState() {
        return state;
    }

    /**
     * Returns the current state count.
     *
     * @return The current state count.
     */
    public int getStateCount() {
        return stateCount;
    }

    /**
     * Returns the state timestamp.
     *
     * @return The state timestamp.
     */
    public long getStateTimestamp() {
        return stateTimestamp;
    }

    public abstract static class Builder<E extends StatefulEntity, B extends Builder<E, B>> {

        protected final E entity;

        protected Builder(E entity) {
            this.entity = entity;
        }

        @SuppressWarnings("unchecked")
        public B id(String id) {
            entity.id = id;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B state(int value) {
            entity.state = value;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B stateCount(int value) {
            entity.stateCount = value;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B stateTimestamp(long value) {
            entity.stateTimestamp = value;
            return (B) this;
        }

        public E build() {
            Objects.requireNonNull(entity.id, "id");
            validate();
            return entity;
        }

        public abstract void validate();
    }
}
