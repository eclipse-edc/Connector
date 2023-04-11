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

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;

import static org.eclipse.edc.connector.callback.dispatcher.http.GenericHttpRemoteDispatcherImpl.CALLBACK_EVENT_HTTP;
import static org.mockito.ArgumentMatchers.argThat;
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
        verify(registry).register(argThat(dispatcher(CALLBACK_EVENT_HTTP)));
    }

    private ArgumentMatcher<GenericHttpRemoteDispatcherImpl> dispatcher(String scheme) {
        return dispatcher -> dispatcher.protocol().equals(scheme);
    }
}
