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

package org.eclipse.dataspaceconnector.spi.entity;

/**
 * Makes an {@link Entity} <em>mutable</em> by adding a {@code updatedAt} field to it, so that changes can be tracked.
 */
public abstract class MutableEntity extends Entity {
    protected long updatedAt;

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long epochMillis) {
        updatedAt = epochMillis;
    }

    /**
     * Updates the updatedAt field to the current epoch in milliseconds, using the clock.
     */
    public void setModified() {
        setUpdatedAt(clock.millis());
    }


    protected abstract static class Builder<T extends MutableEntity, B extends Builder<T, B>> extends Entity.Builder<T, B> {

        protected Builder(T entity) {
            super(entity);
        }

        public B updatedAt(long time) {
            entity.updatedAt = time;
            return self();
        }

        protected T build() {
            super.build();
            if (entity.updatedAt == 0) {
                entity.updatedAt = entity.createdAt;
            }
            return entity;
        }
    }
}
