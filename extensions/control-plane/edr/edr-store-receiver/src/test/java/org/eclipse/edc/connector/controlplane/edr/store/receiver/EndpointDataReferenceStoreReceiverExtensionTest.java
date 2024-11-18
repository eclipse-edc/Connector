/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.edr.store.receiver;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessEvent;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class EndpointDataReferenceStoreReceiverExtensionTest {

    private final EventRouter eventRouter = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(EventRouter.class, eventRouter);
    }

    @Test
    void initialize(ServiceExtensionContext context, EndpointDataReferenceStoreReceiverExtension extension) {
        extension.initialize(context);
        verify(eventRouter).register(eq(TransferProcessEvent.class), isA(EndpointDataReferenceStoreReceiver.class));
        verify(eventRouter, never()).registerSync(any(), any());
    }

    @Test
    void initialize_withSyncConfig(ServiceExtensionContext context, ObjectFactory objectFactory) {

        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("edc.edr.receiver.sync", "true")));

        objectFactory.constructInstance(EndpointDataReferenceStoreReceiverExtension.class).initialize(context);
        verify(eventRouter).registerSync(eq(TransferProcessEvent.class), isA(EndpointDataReferenceStoreReceiver.class));
        verify(eventRouter, never()).register(any(), any());
    }
}
