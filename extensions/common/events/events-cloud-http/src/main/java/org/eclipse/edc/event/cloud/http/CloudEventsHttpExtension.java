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

package org.eclipse.edc.event.cloud.http;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.time.Clock;

@Extension(value = "Cloud events HTTP")
public class CloudEventsHttpExtension implements ServiceExtension {

    @Setting(required = true)
    static final String EDC_EVENTS_CLOUDEVENTS_ENDPOINT = "edc.events.cloudevents.endpoint";

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private EventRouter eventRouter;

    @Inject
    private TypeManager typeManager;

    @Inject
    private Clock clock;

    @Inject
    private Hostname hostname;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var endpoint = context.getConfig().getString(EDC_EVENTS_CLOUDEVENTS_ENDPOINT);

        eventRouter.register(Event.class, new CloudEventsPublisher(endpoint, context.getMonitor(), typeManager, httpClient, clock, hostname));
    }

}
