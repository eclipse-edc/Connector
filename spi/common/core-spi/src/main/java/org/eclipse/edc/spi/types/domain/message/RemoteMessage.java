/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.spi.types.domain.message;

import java.util.Objects;

/**
 * A remote message that is to be sent to another system. Dispatchers are responsible for binding the remote message to
 * the specific transport protocol specified by the message.
 */
public abstract class RemoteMessage {

    protected RemoteMessage() {

    }

    protected String protocol;
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
     * Returns the recipient's callback address.
     */
    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    public abstract static class Builder<RM extends RemoteMessage, B extends Builder<RM, B>> {

        protected RM message;

        protected Builder(RM message) {
            this.message = message;
        }

        public abstract B self();

        public RM build() {
            return message;
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
