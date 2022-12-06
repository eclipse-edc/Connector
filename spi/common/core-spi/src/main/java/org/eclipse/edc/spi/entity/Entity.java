/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.spi.entity;

import java.time.Clock;
import java.util.Objects;

/**
 * Base class for all business objects / entities in EDC that can be persisted. By default, all inheritors are regarded
 * immutable.
 */
public abstract class Entity {
    protected String id;
    protected Clock clock;
    protected long createdAt;

    public long getCreatedAt() {
        return createdAt;
    }

    public String getId() {
        return id;
    }

    public abstract static class Builder<T extends Entity, B extends Builder<T, B>> {
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

        public B createdAt(long value) {
            entity.createdAt = value;
            return self();
        }

        public abstract B self();

        protected T build() {
            entity.clock = Objects.requireNonNullElse(entity.clock, Clock.systemUTC());

            if (entity.createdAt == 0) {
                entity.createdAt = entity.clock.millis();
            }

            return entity;
        }
    }
}
