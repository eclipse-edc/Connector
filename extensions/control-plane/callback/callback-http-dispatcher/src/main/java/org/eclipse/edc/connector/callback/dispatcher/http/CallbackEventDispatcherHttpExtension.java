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

package org.eclipse.edc.connector.callback.dispatcher.http;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

@Extension(value = CallbackEventDispatcherHttpExtension.NAME)
public class CallbackEventDispatcherHttpExtension implements ServiceExtension {


    public static final String NAME = "Callback dispatcher http extension";
    @Inject
    RemoteMessageDispatcherRegistry registry;
    @Inject
    EdcHttpClient client;
    @Inject
    TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        var baseDispatcher = new GenericHttpRemoteDispatcherImpl(client);
        baseDispatcher.registerDelegate(new CallbackEventRemoteMessageDispatcher(typeManager.getMapper()));

        registry.register(new GenericHttpRemoteDispatcherWrapper(baseDispatcher, "http"));
        registry.register(new GenericHttpRemoteDispatcherWrapper(baseDispatcher, "https"));
    }
}
