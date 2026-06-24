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

package org.eclipse.edc.connector.controlplane.asset.spi.event;

import org.eclipse.edc.spi.event.Event;

import java.util.Objects;

/**
 * Class as organizational between level to catch events of type Asset to catch them together in an Event Subscriber
 * Contains data related to assets
 */
public abstract class AssetEvent extends Event {

    protected String assetId;
    protected String participantContextId;

    public String getAssetId() {
        return assetId;
    }

    public String getParticipantContextId() {
        return participantContextId;
    }

    public abstract static class Builder<T extends AssetEvent, B extends AssetEvent.Builder<T, B>> {

        protected final T event;

        protected Builder(T event) {
            this.event = event;
        }

        public abstract B self();

        public B assetId(String assetId) {
            event.assetId = assetId;
            return self();
        }

        public B participantContextId(String participantContextId) {
            event.participantContextId = participantContextId;
            return self();
        }

        public T build() {
            Objects.requireNonNull(event.assetId);
            return event;
        }
    }
}
