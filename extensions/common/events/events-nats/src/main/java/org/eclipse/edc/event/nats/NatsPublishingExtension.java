/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.event.nats;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.api.RetentionPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.io.IOException;
import java.time.Duration;

import static org.eclipse.edc.event.nats.NatsPublishingExtension.NAME;

@Extension(value = NAME)
public class NatsPublishingExtension implements ServiceExtension {
    public static final String NAME = "NATS Event Publishing Extension";
    private static final int ERR_STREAM_NAME_IN_USE = 10058;

    @Configuration
    private NatsConfig natsConfig;

    @Inject
    private EventRouter eventRouter;

    @Inject
    private Monitor monitor;

    // authentication options can be contributed from the outside. Note that the 'server' will be overwritten!
    @Inject(required = false)
    private Options authenticationOptions;

    @Inject
    private TypeManager typeManager;

    @Inject
    private Hostname hostname;

    private Connection natsConnection;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void prepare() {
        try {
            var natsEventPublisher = new NatsEventPublisher(monitor.withPrefix("NATS events"),
                    natsConnection.jetStream(), typeManager.getMapper(), hostname.get());
            eventRouter.register(Event.class, natsEventPublisher);
        } catch (IOException e) {
            throw new EdcException("Error connecting to NATS", e);
        }

    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var builder = authenticationOptions != null ? new Options.Builder(authenticationOptions) : new Options.Builder();

        var optionsBuilder = builder
                .server(natsConfig.natsUrl)
                .maxReconnects(-1)
                .reconnectWait(Duration.ofSeconds(1))
                .maxPingsOut(5)
                .pingInterval(Duration.ofSeconds(20));

        try {
            natsConnection = Nats.connect(optionsBuilder.build());
        } catch (IOException e) {
            monitor.severe("Error connecting to NATS", e);
            throw new EdcException("Error connecting to NATS", e);
        } catch (InterruptedException e) {
            throw new EdcException(e);
        }
        if (natsConfig.createStream) {
            try {
                createStream(natsConfig, natsConnection);
            } catch (Exception e) {
                monitor.severe("Error creating NATS stream", e);
                throw new EdcException("Error creating NATS stream", e);
            }
        }

    }

    @Override
    public void shutdown() {
        if (natsConnection != null) {
            try {
                natsConnection.close();
            } catch (InterruptedException e) {
                monitor.warning("Error closing NATS connection during shutdown", e);
            }
        }
    }

    private void createStream(NatsConfig natsConfig, Connection natsConnection) throws Exception {
        var jsm = natsConnection.jetStreamManagement();
        var streamConfig = StreamConfiguration.builder()
                .name(natsConfig.natsStreamName)
                .subjects(Constants.EVENTS_PREFIX)
                .storageType(StorageType.Memory)
                .retentionPolicy(RetentionPolicy.Interest) // messages are removed only when _all_ subscribers have ack'd
                .build();
        try {
            jsm.addStream(streamConfig);
        } catch (JetStreamApiException e) {
            if (e.getApiErrorCode() == ERR_STREAM_NAME_IN_USE) {
                if (natsConfig.createStreamForce) {
                    monitor.debug("NATS stream '%s' already exists, deleting and recreating".formatted(natsConfig.natsStreamName));
                    jsm.deleteStream(natsConfig.natsStreamName);
                    jsm.addStream(streamConfig);
                } else {
                    var msg = "NATS stream already exists and force create ('edc.events.nats.stream.create.force') is disabled";
                    monitor.severe(msg);
                    throw new EdcException(msg, e);
                }
            } else {
                throw new EdcException("Error creating NATS stream", e);
            }
        }
    }

    @Settings
    public record NatsConfig(
            @Setting(key = "edc.events.nats.url", description = "The URL of the NATS server'") String natsUrl,
            @Setting(key = "edc.events.nats.stream", description = "The name of the NATS stream to use for event publishing'") String natsStreamName,
            @Setting(key = "edc.events.nats.stream.create", required = false, description = "If the stream should be attempted to be created. May fail if the stream exists.'", defaultValue = "true") boolean createStream,
            @Setting(key = "edc.events.nats.stream.create.force", required = false, description = "If the stream should be created, and overwritten if it exists'", defaultValue = "true") boolean createStreamForce) {
    }
}
