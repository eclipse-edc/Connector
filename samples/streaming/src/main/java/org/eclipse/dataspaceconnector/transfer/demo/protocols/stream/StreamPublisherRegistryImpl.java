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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.StreamPublisher;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.StreamPublisherRegistry;
import okhttp3.OkHttpClient;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class StreamPublisherRegistryImpl implements StreamPublisherRegistry {
    private Vault vault;
    private OkHttpClient httpClient;
    private ObjectMapper objectMapper;
    private Monitor monitor;

    private List<StreamPublisher> publishers = new ArrayList<>();

    public StreamPublisherRegistryImpl(Vault vault, OkHttpClient httpClient, ObjectMapper objectMapper, Monitor monitor) {
        this.vault = vault;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.monitor = monitor;
    }

    @Override
    public void register(StreamPublisher publisher) {
        var context = new PushStreamContext(vault, httpClient, objectMapper, monitor);
        publisher.initialize(context);
        publishers.add(publisher);
    }

    @Override
    public void notifyPublisher(DataRequest data) {
        for (StreamPublisher publisher : publishers) {
            if (publisher.canHandle(data)) {
                publisher.notifyPublisher(data);
                return;
            }
        }
        throw new EdcException("No stream publisher found for request: " + data.getId());
    }
}
