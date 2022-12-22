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

package org.eclipse.edc.spi.event.asset;

import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventPayload;

import java.util.Objects;

/**
 *  Class as organizational between level to catch events of type TransferProcess to catch them together in an Event Subscriber
 *  Contains data related to assets
 */
public abstract class AssetEvent<P extends AssetEvent.Payload> extends Event<P> {

    public static abstract class Payload extends EventPayload {
        protected String assetId;

        public String getAssetId() {
            return assetId;
        }
    }

    public static class Builder<E extends AssetEvent<P>, P extends AssetEvent.Payload, B extends Builder<E, P, B>> extends Event.Builder<E, P, B> {

        protected Builder(E event, P payload) {
            super(event, payload);
        }

        @SuppressWarnings("unchecked")
        public B assetId(String assetId) {
            event.payload.assetId = assetId;
            return (B) this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(event.payload.assetId);
        }

    }
}
