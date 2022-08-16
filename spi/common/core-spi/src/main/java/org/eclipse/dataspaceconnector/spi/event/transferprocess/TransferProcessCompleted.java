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

package org.eclipse.dataspaceconnector.spi.event.transferprocess;

import org.eclipse.dataspaceconnector.spi.event.Event;
import org.eclipse.dataspaceconnector.spi.event.EventPayload;

import java.util.Objects;

/**
 * This event is raised when the TransferProcess has been completed.
 */
public class TransferProcessCompleted extends Event<TransferProcessCompleted.Payload> {

    private TransferProcessCompleted() {
    }

    public static class Builder extends Event.Builder<TransferProcessCompleted, Payload, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new TransferProcessCompleted(), new Payload());
        }

        public Builder transferProcessId(String transferProcessId) {
            event.payload.transferProcessId = transferProcessId;
            return this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(event.payload.transferProcessId);
        }
    }

    public static class Payload extends EventPayload {
        private String transferProcessId;

        public String getTransferProcessId() {
            return transferProcessId;
        }
    }
}
