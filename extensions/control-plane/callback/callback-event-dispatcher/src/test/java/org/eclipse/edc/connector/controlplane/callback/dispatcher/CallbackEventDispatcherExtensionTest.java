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

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
public class CallbackEventDispatcherExtensionTest {

    EventRouter router = mock(EventRouter.class);

    private CallbackEventDispatcherExtension extension;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(EventRouter.class, router);
        context.registerService(RemoteMessageDispatcherRegistry.class, mock(RemoteMessageDispatcherRegistry.class));

        extension = factory.constructInstance(CallbackEventDispatcherExtension.class);
    }

    @Test
    void initialize_shouldRegisterBothListeners(ServiceExtensionContext context) {

        extension.initialize(context);

        verify(router).register(eq(Event.class), argThat(callbackEventDispatcherMatcher(false)));
        verify(router).registerSync(eq(Event.class), argThat(callbackEventDispatcherMatcher(true)));

    }

    private ArgumentMatcher<CallbackEventDispatcher> callbackEventDispatcherMatcher(boolean transactional) {
        return dispatcher -> dispatcher.isTransactional() == transactional;
    }
}
