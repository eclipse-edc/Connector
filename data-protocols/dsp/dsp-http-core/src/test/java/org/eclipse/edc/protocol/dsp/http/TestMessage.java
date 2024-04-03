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

import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

public record TestMessage(String protocol, String counterPartyAddress, String counterPartyId) implements RemoteMessage {
    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    @Override
    public String getCounterPartyId() {
        return counterPartyId;
    }
}
