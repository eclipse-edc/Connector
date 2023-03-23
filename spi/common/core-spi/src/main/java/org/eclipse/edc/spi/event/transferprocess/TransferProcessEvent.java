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

import java.util.Objects;

/**
 *  Class as organizational between level to catch events of type TransferProcess to catch them together in an Event Subscriber
 *  Contains data related to transfer processes
 */
public abstract class TransferProcessEvent extends Event {

    protected String transferProcessId;

    public String getTransferProcessId() {
        return transferProcessId;
    }

    public abstract static class Builder<T extends TransferProcessEvent, B extends TransferProcessEvent.Builder<T, B>> {

        protected final T event;

        protected Builder(T event) {
            this.event = event;
        }

        public B transferProcessId(String transferProcessId) {
            event.transferProcessId = transferProcessId;
            return (B) this;
        }

        public abstract B self();
        
        public T build() {
            Objects.requireNonNull(event.transferProcessId);
            return event;
        }

    }

}
