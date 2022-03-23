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
package org.eclipse.dataspaceconnector.core;

import org.eclipse.dataspaceconnector.policy.model.PolicyRegistrationTypes;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoreServicesExtensionTest {
    private CoreServicesExtension extension;
    private ServiceExtensionContext context;
    private TypeManager typeManager;

    @Test
    void verifyPolicyTypesAreRegistered() {
        extension.initialize(context);
        PolicyRegistrationTypes.TYPES.forEach(t -> verify(typeManager).registerTypes(t));
    }

    @BeforeEach
    void setUp() {
        extension = new CoreServicesExtension();
        typeManager = spy(new TypeManager());
        context = mock(ServiceExtensionContext.class);
        when(context.getSetting(eq(CoreServicesExtension.MAX_RETRIES), anyInt())).thenReturn(1);
        when(context.getSetting(eq(CoreServicesExtension.BACKOFF_MIN_MILLIS), anyInt())).thenReturn(1);
        when(context.getSetting(eq(CoreServicesExtension.BACKOFF_MAX_MILLIS), anyInt())).thenReturn(2);
        when(context.getService(eq(PrivateKeyResolver.class))).thenReturn(mock(PrivateKeyResolver.class));
        when(context.getTypeManager()).thenReturn(typeManager);
    }
}
