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

package org.eclipse.edc.connector.controlplane.callback.dispatcher;

import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackRegistry;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

@Extension(value = CallbackEventDispatcherExtension.NAME)
public class CallbackEventDispatcherExtension implements ServiceExtension {

    public static final String NAME = "Callback dispatcher extension";

    @Inject
    EventRouter router;
    @Inject
    Monitor monitor;
    @Inject
    CallbackRegistry callbackRegistry;
    @Inject
    EdcHttpClient edcHttpClient;
    @Inject
    TypeManager typeManager;
    @Inject
    Vault vault;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var callbackClient = new CallbackHttpClient(edcHttpClient, typeManager.getMapper(), vault);
        router.registerSync(Event.class, new CallbackEventDispatcher(callbackClient, callbackRegistry, true, monitor));
        router.register(Event.class, new CallbackEventDispatcher(callbackClient, callbackRegistry, false, monitor));
    }
}
