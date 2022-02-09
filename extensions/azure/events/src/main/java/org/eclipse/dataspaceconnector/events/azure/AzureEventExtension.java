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
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessObservable;

import java.util.Objects;

public class AzureEventExtension implements ServiceExtension {

    private Monitor monitor;

    @Override
    public String name() {
        return "Azure Events";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        monitor.info("AzureEventExtension: create event grid appender");
        registerListeners(context);
    }

    private void registerListeners(ServiceExtensionContext context) {

        var vault = context.getService(Vault.class);

        var config = new AzureEventGridConfig(context);
        var topicName = config.getTopic();
        var endpoint = config.getEndpoint(topicName);
        monitor.info("AzureEventExtension: will use topic endpoint " + endpoint);

        var publisherClient = new EventGridPublisherClientBuilder()
                .credential(new AzureKeyCredential(Objects.requireNonNull(vault.resolveSecret(topicName), "Did not find secret in vault: " + endpoint)))
                .endpoint(endpoint)
                .buildEventGridEventPublisherAsyncClient();


        AzureEventGridPublisher publisher = new AzureEventGridPublisher(context.getConnectorId(), monitor, publisherClient);

        var processObservable = context.getService(TransferProcessObservable.class, true);
        if (processObservable != null) {
            processObservable.registerListener(publisher);
        }
    }


}
