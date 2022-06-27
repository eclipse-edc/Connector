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

package org.eclipse.dataspaceconnector.spi.event.asset;

import org.eclipse.dataspaceconnector.spi.event.Event;
import org.eclipse.dataspaceconnector.spi.event.EventPayload;

import java.util.Objects;

/**
 * Describe a new Asset creation, after this has emitted, an Asset with a certain id will be available.
 */
public class AssetCreated extends Event<AssetCreated.Payload> {

    private AssetCreated() {
    }

    public static class Builder extends Event.Builder<AssetCreated, Payload> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new AssetCreated(), new Payload());
        }

        public Builder assetId(String assetId) {
            event.payload.assetId = assetId;
            return this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(event.payload.assetId);
        }
    }

    public static class Payload extends EventPayload {
        private String assetId;

        public String getAssetId() {
            return assetId;
        }
    }

}
