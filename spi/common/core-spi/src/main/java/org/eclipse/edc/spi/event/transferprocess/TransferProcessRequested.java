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
 * This event is raised when the TransferProcess has been requested to the provider.
 */
public class TransferProcessRequested extends TransferProcessEvent<TransferProcessRequested.Payload> {

    private TransferProcessRequested() {
    }

    /**
     * This class contains all event specific attributes of a TransferProcess Requested Event
     *
     */
    public static class Payload extends TransferProcessEvent.Payload {
    }

    public static class Builder extends TransferProcessEvent.Builder<TransferProcessRequested, Payload, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new TransferProcessRequested(), new Payload());
        }

    }

}
