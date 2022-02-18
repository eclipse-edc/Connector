/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.sync;

import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.iam.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiverRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformer;
import org.eclipse.dataspaceconnector.transfer.sync.transformer.ProxyEndpointDataReferenceTransformer;

/**
 * This extension overrides the default implementation of the {@link EndpointDataReferenceTransformer} that is registered
 * by the core transfer extensions, so that the consumer data plane can be used as proxy to query the data.
 */
@Provides(EndpointDataReferenceTransformer.class)
public class SyncDataTransferConsumerExtension implements ServiceExtension {

    @EdcSetting
    private static final String CONSUMER_DATA_PROXY_ADDRESS = "edc.transfer.sync.consumer-proxy.address";

    @Inject
    private TokenGenerationService tokenGenerationService;

    @Inject
    private EndpointDataReferenceReceiverRegistry receiverRegistry;

    @Override
    public String name() {
        return "Sync Data Transfer Consumer";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var dataEndpoint = context.getSetting(CONSUMER_DATA_PROXY_ADDRESS, "/api/public/transfer");
        var consumerProxyTransformer = new ProxyEndpointDataReferenceTransformer(tokenGenerationService, dataEndpoint, context.getTypeManager());
        context.registerService(EndpointDataReferenceTransformer.class, consumerProxyTransformer);
    }
}
