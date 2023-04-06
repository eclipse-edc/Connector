/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.callback.dispatcher;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Extension(value = CallbackEventDispatcherExtension.NAME)
public class CallbackEventDispatcherExtension implements ServiceExtension {

    public static final String NAME = "Callback dispatcher extension";
    
    @Inject
    RemoteMessageDispatcherRegistry dispatcherRegistry;

    @Inject
    EventRouter router;

    @Inject
    Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        // Event listener for invoking callbacks in sync (transactional) and async (not transactional)
        router.registerSync(Event.class, new CallbackEventDispatcher<>(dispatcherRegistry, true, monitor));
        router.register(Event.class, new CallbackEventDispatcher<>(dispatcherRegistry, false, monitor));

    }
}
