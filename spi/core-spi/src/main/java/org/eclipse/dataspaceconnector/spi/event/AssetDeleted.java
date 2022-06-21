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

import java.util.Objects;

/**
 * Describe an Asset deletion, after this has emitted, the Asset represented by the id won't be available anymore.
 */
public class AssetDeleted extends Event {

    private String id;

    private AssetDeleted() {
    }

    public String getId() {
        return id;
    }

    public static class Builder extends Event.Builder<AssetDeleted> {

        public static Builder newInstance() {
            return new Builder(new AssetDeleted());
        }

        private Builder(AssetDeleted event) {
            super(event);
        }

        public Builder id(String id) {
            event.id = id;
            return this;
        }

        @Override
        public AssetDeleted build() {
            super.build();
            Objects.requireNonNull(event.id);
            return event;
        }
    }
}
