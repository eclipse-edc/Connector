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

package org.eclipse.edc.spi.event.transferprocess;

import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventPayload;

import java.util.Objects;

/**
 *  Class as organizational between level to catch events of type TransferProcess to catch them together in an Event Subscriber
 *  Contains data related to transfer processes
 */
public abstract class TransferProcessEvent<P extends TransferProcessEvent.Payload> extends Event<P> {

    public abstract static class Payload extends EventPayload {
        protected String transferProcessId;

        public String getTransferProcessId() {
            return transferProcessId;
        }
    }

    public static class Builder<E extends TransferProcessEvent<P>, P extends TransferProcessEvent.Payload, B extends Builder<E, P, B>> extends Event.Builder<E, P, B> {

        protected Builder(E event, P payload) {
            super(event, payload);
        }

        @SuppressWarnings("unchecked")
        public B transferProcessId(String transferProcessId) {
            event.payload.transferProcessId = transferProcessId;
            return (B) this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(event.payload.transferProcessId);
        }

    }
}
