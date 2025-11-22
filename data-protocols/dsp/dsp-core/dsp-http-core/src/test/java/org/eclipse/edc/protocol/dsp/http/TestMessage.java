/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.protocol.dsp.http;

import org.eclipse.edc.spi.types.domain.message.ProtocolRemoteMessage;

import java.util.Objects;

public final class TestMessage extends ProtocolRemoteMessage {

    public TestMessage(String protocol, String counterPartyAddress, String counterPartyId) {
        this.protocol = protocol;
        this.counterPartyAddress = counterPartyAddress;
        this.counterPartyId = counterPartyId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TestMessage) obj;
        return Objects.equals(this.protocol, that.protocol) &&
                Objects.equals(this.counterPartyAddress, that.counterPartyAddress) &&
                Objects.equals(this.counterPartyId, that.counterPartyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, counterPartyAddress, counterPartyId);
    }

    @Override
    public String toString() {
        return "TestMessage[" +
                "protocol=" + protocol + ", " +
                "counterPartyAddress=" + counterPartyAddress + ", " +
                "counterPartyId=" + counterPartyId + ']';
    }

}
