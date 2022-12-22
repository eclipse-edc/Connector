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
 *  This event is raised when the TransferProcess has been provisioned.
 */
public class TransferProcessProvisioned extends TransferProcessEvent<TransferProcessProvisioned.Payload> {

    private TransferProcessProvisioned() {
    }

    /**
     * This class contains all event specific attributes of a TransferProcess Provisioned Event
     *
     */
    public static class Payload extends TransferProcessEvent.Payload {
    }

    public static class Builder extends TransferProcessEvent.Builder<TransferProcessProvisioned, Payload, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new TransferProcessProvisioned(), new Payload());
        }
    }

}
