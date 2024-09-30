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

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.connector.controlplane.callback.dispatcher.http.GenericHttpRemoteDispatcherImpl.CALLBACK_EVENT_HTTP;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
public class GenericHttpRemoteDispatcherWrapperExtensionTest {

    private final RemoteMessageDispatcherRegistry registry = mock(RemoteMessageDispatcherRegistry.class);
    private CallbackEventDispatcherHttpExtension extension;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(TypeManager.class, mock(TypeManager.class));
        context.registerService(EdcHttpClient.class, mock(EdcHttpClient.class));
        context.registerService(RemoteMessageDispatcherRegistry.class, registry);

        extension = factory.constructInstance(CallbackEventDispatcherHttpExtension.class);
    }


    @Test
    void initialize_shouldRegisterBothDispatcher(ServiceExtensionContext context) {

        extension.initialize(context);
        verify(registry).register(eq(CALLBACK_EVENT_HTTP), isA(GenericHttpRemoteDispatcherImpl.class));
    }

}
