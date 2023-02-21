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

/**
 * This event is raised when the TransferProcess has been cancelled.
 *
 * @deprecated this event is not thrown by anyone, please use {@link TransferProcessTerminated} instead
 */
@Deprecated(since = "milestone9")
public class TransferProcessCancelled extends TransferProcessEvent<TransferProcessCancelled.Payload> {

    private TransferProcessCancelled() {
    }

    /**
     * This class contains all event specific attributes of a TransferProcess Cancelled Event
     */
    public static class Payload extends TransferProcessEvent.Payload {
    }

    public static class Builder extends TransferProcessEvent.Builder<TransferProcessCancelled, Payload, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new TransferProcessCancelled(), new Payload());
        }

    }
}
