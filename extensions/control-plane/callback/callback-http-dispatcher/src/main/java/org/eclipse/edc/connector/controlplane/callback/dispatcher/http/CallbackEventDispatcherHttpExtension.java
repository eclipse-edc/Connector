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

package org.eclipse.edc.connector.controlplane.callback.dispatcher.http;

import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackProtocolResolverRegistry;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import static org.eclipse.edc.connector.controlplane.callback.dispatcher.http.GenericHttpRemoteDispatcherImpl.CALLBACK_EVENT_HTTP;

@Extension(value = CallbackEventDispatcherHttpExtension.NAME)
public class CallbackEventDispatcherHttpExtension implements ServiceExtension {

    public static final String NAME = "Callback dispatcher http extension";
    @Inject
    private RemoteMessageDispatcherRegistry registry;
    @Inject
    private EdcHttpClient client;
    @Inject
    private TypeManager typeManager;

    @Inject
    private CallbackProtocolResolverRegistry resolverRegistry;

    @Inject
    private Vault vault;


    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        resolverRegistry.registerResolver(this::resolveScheme);

        var baseDispatcher = new GenericHttpRemoteDispatcherImpl(client);
        baseDispatcher.registerDelegate(new CallbackEventRemoteMessageDispatcher(typeManager.getMapper(), vault));

        registry.register(CALLBACK_EVENT_HTTP, baseDispatcher);
    }


    private String resolveScheme(String scheme) {
        if (scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http")) {
            return CALLBACK_EVENT_HTTP;
        }
        return null;
    }
}
