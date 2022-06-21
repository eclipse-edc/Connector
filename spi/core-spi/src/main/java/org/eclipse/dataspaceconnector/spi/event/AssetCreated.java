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
 * Describe a new Asset creation, after this has emitted, an Asset with a certain id will be available.
 */
public class AssetCreated extends Event {

    private String id;

    private AssetCreated() {
    }

    public String getId() {
        return id;
    }

    public static class Builder extends Event.Builder<AssetCreated> {

        public static Builder newInstance() {
            return new Builder(new AssetCreated());
        }

        private Builder(AssetCreated event) {
            super(event);
        }

        public Builder id(String id) {
            event.id = id;
            return this;
        }

        @Override
        public AssetCreated build() {
            super.build();
            Objects.requireNonNull(event.id);
            return event;
        }
    }
}
