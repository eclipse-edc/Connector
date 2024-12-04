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

package org.eclipse.edc.connector.controlplane.services.protocol;

import org.eclipse.edc.connector.controlplane.protocolversion.spi.ProtocolVersionRequest;
import org.eclipse.edc.connector.controlplane.protocolversion.spi.ProtocolVersionRequestMessage;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionService;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.response.StatusResult;

import java.util.concurrent.CompletableFuture;

public class VersionServiceImpl implements VersionService {

    private final RemoteMessageDispatcherRegistry dispatcher;

    public VersionServiceImpl(RemoteMessageDispatcherRegistry dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public CompletableFuture<StatusResult<byte[]>> requestVersions(ProtocolVersionRequest request) {
        var message = ProtocolVersionRequestMessage.Builder.newInstance()
                .protocol(request.getProtocol())
                .counterPartyId(request.getCounterPartyId())
                .counterPartyAddress(request.getCounterPartyAddress())
                .build();

        return dispatcher.dispatch(byte[].class, message);
    }
}
