/*
 *  Copyright (c) 2024 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.secret.spi.event;

import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventPayload;

import java.util.Objects;

/**
 * Class as organizational between level to catch events of type Secret to catch them together in an Event Subscriber
 * Contains data related to secretss
 */
public abstract class SecretEvent extends Event {

    protected String secretId;

    public String getSecretId() {
        return secretId;
    }


    public abstract static class Payload extends EventPayload {
        protected String secretId;

        public String getSecretId() {
            return secretId;
        }
    }

    public abstract static class Builder<T extends SecretEvent, B extends SecretEvent.Builder<T, B>> {

        protected final T event;

        protected Builder(T event) {
            this.event = event;
        }

        public abstract B self();

        public B secretId(String secretId) {
            event.secretId = secretId;
            return self();
        }

        public T build() {
            Objects.requireNonNull(event.secretId);
            return event;
        }
    }
}
