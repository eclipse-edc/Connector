/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.core;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.core.event.EventExecutorServiceContainer;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.Executors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class CoreServicesExtensionTest {
    private final TypeManager typeManager = mock(TypeManager.class);
    private CoreServicesExtension extension;
    private ServiceExtensionContext context;
    private PrivateKeyResolver privateKeyResolverMock;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(EventExecutorServiceContainer.class, new EventExecutorServiceContainer(Executors.newSingleThreadExecutor()));
        context.registerService(TypeManager.class, typeManager);

        privateKeyResolverMock = mock(PrivateKeyResolver.class);
        context.registerService(PrivateKeyResolver.class, privateKeyResolverMock);

        context.registerService(ExecutorInstrumentation.class, mock(ExecutorInstrumentation.class));

        this.context = context;
        extension = factory.constructInstance(CoreServicesExtension.class);
    }

    @Test
    void verifyPolicyTypesAreRegistered() {
        extension.initialize(context);
        extension.prepare();
        PolicyRegistrationTypes.TYPES.forEach(t -> verify(typeManager).registerTypes(t));
    }

}
