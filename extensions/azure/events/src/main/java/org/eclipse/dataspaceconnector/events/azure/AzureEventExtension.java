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
package org.eclipse.dataspaceconnector.events.azure;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;
import org.eclipse.dataspaceconnector.spi.metadata.MetadataObservable;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessObservable;

import java.util.Objects;
import java.util.Set;

import static org.eclipse.dataspaceconnector.common.settings.SettingsHelper.getConnectorId;

public class AzureEventExtension implements ServiceExtension {

    private static final String DEFAULT_TOPIC_NAME = "connector-events";
    private static final String DEFAULT_ENDPOINT_NAME_TEMPLATE = "https://%s.westeurope-1.eventgrid.azure.net/api/events";
    private static final String TOPIC_NAME_SETTING = "edc.events.topic.name";
    private static final String TOPIC_ENDPOINT_SETTING = "edc.events.topic.endpoint";
    private Monitor monitor;

    @Override
    public Set<String> requires() {
        return Set.of("dataspaceconnector:transfer-process-observable");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        monitor.info("AzureEventExtension: create event grid appender");
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


        AzureEventGridPublisher publisher = new AzureEventGridPublisher(getConnectorId(context), monitor, publisherClient);

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
        return context.getSetting(AzureEventExtension.TOPIC_NAME_SETTING, AzureEventExtension.DEFAULT_TOPIC_NAME);
    }

    private String getEndpoint(ServiceExtensionContext context, String topicName) {
        var ep = context.getSetting(AzureEventExtension.TOPIC_ENDPOINT_SETTING, null);
        if (ep == null) {
            ep = String.format(AzureEventExtension.DEFAULT_ENDPOINT_NAME_TEMPLATE, topicName);
        }
        return ep;
    }
}
