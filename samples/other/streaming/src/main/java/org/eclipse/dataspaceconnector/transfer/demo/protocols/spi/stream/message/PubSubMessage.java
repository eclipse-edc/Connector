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

package org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.dataspaceconnector.spi.types.domain.Polymorphic;

/**
 * The base message for the streaming demo pub/sub protocol.
 * <p>
 * This class demonstrates the use of polymorphic JSON deserialization. Subclasses are registered with the type manager so that they canbe automatically deserialized.
 */
public abstract class PubSubMessage implements Polymorphic {
    protected Protocol protocol;

    protected PubSubMessage(Protocol protocol) {
        this.protocol = protocol;
    }

    @JsonIgnore
    public Protocol getProtocol() {
        return protocol;
    }

    public enum Protocol {
        SUBSCRIBE, UNSUBSCRIBE, CONNECT, DISCONNECT, PUBLISH, DATA
    }

}
