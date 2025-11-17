/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.spi.types.domain.message;

/**
 * Envelope that represent a message that is sent through the Dataspace Protocol
 */
public abstract class ProtocolRemoteMessage extends RemoteMessage {

    protected String counterPartyId;

    /**
     * Returns the recipient's id.
     */
    public String getCounterPartyId() {
        return counterPartyId;
    }

    public abstract static class Builder<RM extends ProtocolRemoteMessage, B extends Builder<RM, B>> extends RemoteMessage.Builder<RM, B> {

        protected Builder(RM message) {
            super(message);
        }

        public B counterPartyId(String counterPartyId) {
            message.counterPartyId = counterPartyId;
            return self();
        }

    }

}
