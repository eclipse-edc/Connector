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

package org.eclipse.edc.controlplane;

import java.util.Objects;

/**
 * Envelope that represent a message that is sent through the Dataspace Protocol
 */
public abstract class ProtocolRemoteMessage {

    protected String protocol;
    protected String counterPartyId;
    protected String counterPartyAddress;

    /**
     * Returns the transport protocol this message must be sent over.
     */
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        Objects.requireNonNull(protocol);
        this.protocol = protocol;
    }

    /**
     * Returns the recipient's id.
     */
    public String getCounterPartyId() {
        return counterPartyId;
    }

    /**
     * Returns the recipient's callback address.
     */
    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    public abstract static class Builder<RM extends ProtocolRemoteMessage, B extends Builder<RM, B>> {

        protected RM message;
        
        protected Builder(RM message) {
            this.message = message;
        }

        public abstract B self();

        public RM build() {
            return message;
        }

        public B counterPartyId(String counterPartyId) {
            message.counterPartyId = counterPartyId;
            return self();
        }

        public B protocol(String protocol) {
            message.protocol = protocol;
            return self();
        }

        public B counterPartyAddress(String counterPartyAddress) {
            message.counterPartyAddress = counterPartyAddress;
            return self();
        }

    }

}
