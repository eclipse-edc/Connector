/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.events.cloudevents.http;

import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.event.EventRouter;
import org.eclipse.dataspaceconnector.spi.system.Hostname;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.time.Clock;

public class CloudEventsHttpExtension implements ServiceExtension {

    @EdcSetting(required = true)
    static final String EDC_EVENTS_CLOUDEVENTS_ENDPOINT = "edc.events.cloudevents.endpoint";

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private EventRouter eventRouter;

    @Inject
    private TypeManager typeManager;

    @Inject
    private Clock clock;

    @Inject
    private Hostname hostname;

    @Inject
    private RetryPolicy<Object> retryPolicy;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var endpoint = context.getConfig().getString(EDC_EVENTS_CLOUDEVENTS_ENDPOINT);

        eventRouter.register(new CloudEventsPublisher(endpoint, context.getMonitor(), typeManager, okHttpClient, clock, hostname, retryPolicy));
    }

}
