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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

/**
 * The Event base class, the fields are:
 *  - id: unique identifier of the event (set by default at a random UUID)
 *  - at: creation timestamp
 *  - payload: the data provided by the event
 *  - type: added on serialization, contains the name of the runtime class name
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
public abstract class Event<P extends EventPayload> {

    protected String id;

    protected long at;

    protected P payload;

    public String getId() {
        return id;
    }

    public long getAt() {
        return at;
    }

    public P getPayload() {
        return payload;
    }

    public abstract static class Builder<T extends Event<P>, P extends EventPayload, B extends Builder<T, P, B>> {

        protected final T event;

        protected Builder(T event, P payload) {
            this.event = event;
            this.event.payload = payload;
        }

        @SuppressWarnings("unchecked")
        public B id(String id) {
            event.id = id;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B at(long at) {
            event.at = at;
            return (B) this;
        }

        public T build() {
            if (event.id == null) {
                event.id = UUID.randomUUID().toString();
            }
            if (event.at == 0) {
                throw new IllegalStateException("Event 'at' field must be set");
            }
            validate();
            return event;
        }

        protected abstract void validate();
    }

}
