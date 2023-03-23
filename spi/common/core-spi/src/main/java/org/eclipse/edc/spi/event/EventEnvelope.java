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

package org.eclipse.edc.spi.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

/**
 * The {@link EventEnvelope} is a container for all EDC events. Its responsibility is to carry the {@link Event} as payload
 * , and it also contains the event metadata:
 * - id: unique identifier of the event (set by default at a random UUID)
 * - at: creation timestamp
 * - payload: the data provided by the event
 * - type: added on serialization, contains the name of the runtime class name of the payload
 */
public class EventEnvelope<E extends Event> {

    protected String id;

    protected long at;

    protected E payload;

    public String getId() {
        return id;
    }

    public long getAt() {
        return at;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
    public E getPayload() {
        return payload;
    }

    public static class Builder<E extends Event> {

        protected final EventEnvelope<E> envelope;

        protected Builder() {
            envelope = new EventEnvelope<>();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder<E> id(String id) {
            envelope.id = id;
            return this;
        }

        public Builder<E> at(long at) {
            envelope.at = at;
            return this;
        }

        public Builder<E> payload(E payload) {
            envelope.payload = payload;
            return this;
        }

        public EventEnvelope<E> build() {
            if (envelope.id == null) {
                envelope.id = UUID.randomUUID().toString();
            }
            if (envelope.at == 0) {
                throw new IllegalStateException("Event 'at' field must be set");
            }
            return envelope;
        }

    }

}
