/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */
package com.microsoft.dagx.events.azure;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;
import com.microsoft.dagx.spi.metadata.MetadataObservable;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.TransferProcessObservable;

import java.util.Objects;
import java.util.Set;

public class AzureEventExtension implements ServiceExtension {

    private static final String DEFAULT_TOPIC_NAME = "connector-events";
    private static final String DEFAULT_ENDPOINT_NAME_TEMPLATE = "https://%s.westeurope-1.eventgrid.azure.net/api/events";
    private static final String TOPIC_NAME_SETTING = "dagx.events.topic.name";
    private static final String TOPIC_ENDPOINT_SETTING = "dagx.events.topic.endpoint";
    private Monitor monitor;

    @Override
    public Set<String> requires() {
        return Set.of("dagx:transfer-process-observable");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        monitor.info("AzureEventsExtension: create event grid appender");
        registerListeners(context);

        monitor.info("Initialized Azure Events Extension");
    }


    @Override
    public void start() {
        monitor.info("Started Azure Events Extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown Azure Events Extension");
    }

    private void registerListeners(ServiceExtensionContext context) {

        var vault = context.getService(Vault.class);

        var topicName = getTopic(context);
        var endpoint = getEndpoint(context, topicName);
        monitor.info("AzureEventExtension: will use topic endpoint " + endpoint);

        var publisherClient = new EventGridPublisherClientBuilder()
                .credential(new AzureKeyCredential(Objects.requireNonNull(vault.resolveSecret(topicName), "Did not find secret in vault: " + endpoint)))
                .endpoint(endpoint)
                .buildEventGridEventPublisherAsyncClient();

        final AzureEventGridPublisher publisher = new AzureEventGridPublisher(monitor, publisherClient);

        var processObservable = context.getService(TransferProcessObservable.class, true);
        if (processObservable != null) {
            processObservable.registerListener(publisher);
        }

        var metadataObservable = context.getService(MetadataObservable.class, true);
        if (metadataObservable != null) {
            metadataObservable.registerListener(publisher);
        }


    }

    private String getTopic(ServiceExtensionContext context) {
        return context.getSetting(TOPIC_NAME_SETTING, DEFAULT_TOPIC_NAME);
    }

    private String getEndpoint(ServiceExtensionContext context, String topicName) {
        var ep = context.getSetting(TOPIC_ENDPOINT_SETTING, null);
        if (ep == null) {
            ep = String.format(DEFAULT_ENDPOINT_NAME_TEMPLATE, topicName);
        }
        return ep;
    }
}
