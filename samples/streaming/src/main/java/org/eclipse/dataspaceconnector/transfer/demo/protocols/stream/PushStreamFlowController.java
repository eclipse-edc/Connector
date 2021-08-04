/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.demo.protocols.stream;

import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.DemoProtocols;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.StreamPublisherRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Implements push-style streaming. The client runtime provisions a topic which the provider runtime publishes to.
 */
public class PushStreamFlowController implements DataFlowController {
    private StreamPublisherRegistry publisherRegistry;

    public PushStreamFlowController(StreamPublisherRegistry registry) {
        this.publisherRegistry = registry;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {
        return DemoProtocols.PUSH_STREAM_WS.equals(dataRequest.getDestinationType()) ||  DemoProtocols.PUSH_STREAM_HTTP.equals(dataRequest.getDestinationType()) ;
    }

    @Override
    public @NotNull DataFlowInitiateResponse initiateFlow(DataRequest dataRequest) {
        publisherRegistry.notifyPublisher(dataRequest);
        return DataFlowInitiateResponse.OK;
    }

}
