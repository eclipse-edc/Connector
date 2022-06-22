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

/**
 * The Event base class.
 * It provides a default field "at" that gives the timestamp when the event was created.
 * When serialized to JSON, will add a "type" field with the name of the runtime class name.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
public abstract class Event<P> {

    protected long at;

    protected P payload;

    public long getAt() {
        return at;
    }

    public P getPayload() {
        return payload;
    }

    public abstract static class Builder<T extends Event<P>, P extends EventPayload> {

        protected final T event;

        protected Builder(T event, P payload) {
            this.event = event;
            this.event.payload = payload;
        }

        public Builder<T, P> at(long at) {
            event.at = at;
            return this;
        }

        public T build() {
            if (event.at == 0) {
                throw new IllegalStateException("Event 'at' field must be set");
            }
            validate();
            return event;
        }

        protected abstract void validate();
    }

}
