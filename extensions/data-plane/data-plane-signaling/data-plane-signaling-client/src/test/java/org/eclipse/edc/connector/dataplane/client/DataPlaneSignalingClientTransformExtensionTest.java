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

package org.eclipse.edc.connector.dataplane.client;

import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowStartMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowSuspendMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowTerminateMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataFlowResponseMessageTransformer;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneSignalingClientTransformExtensionTest {

    private final TypeTransformerRegistry signalingRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        TypeTransformerRegistry parentRegistry = mock();
        when(parentRegistry.forContext("signaling-api")).thenReturn(signalingRegistry);
        context.registerService(TypeTransformerRegistry.class, parentRegistry);
    }

    @Test
    void verifyTransformerRegistry(DataPlaneSignalingClientTransformExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(signalingRegistry).register(isA(JsonObjectFromDataFlowSuspendMessageTransformer.class));
        verify(signalingRegistry).register(isA(JsonObjectFromDataFlowTerminateMessageTransformer.class));
        verify(signalingRegistry).register(isA(JsonObjectFromDataFlowStartMessageTransformer.class));
        verify(signalingRegistry).register(isA(JsonObjectToDataFlowResponseMessageTransformer.class));
    }

}
