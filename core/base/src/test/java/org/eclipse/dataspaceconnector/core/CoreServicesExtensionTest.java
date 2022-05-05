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

import org.eclipse.dataspaceconnector.core.security.DefaultPrivateKeyParseFunction;
import org.eclipse.dataspaceconnector.policy.model.PolicyRegistrationTypes;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.injection.ObjectFactory;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.PrivateKey;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(CoreServicesDependencyInjectionExtension.class)
class CoreServicesExtensionTest {
    private CoreServicesExtension extension;
    private ServiceExtensionContext context;
    private TypeManager typeManager;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        typeManager = context.getTypeManager(); //is already a spy!
        context.registerService(ExecutorInstrumentation.class, mock(ExecutorInstrumentation.class));

        this.context = context;
        extension = factory.constructInstance(CoreServicesExtension.class);
    }

    @Test
    void verifyPolicyTypesAreRegistered() {
        extension.initialize(context);
        PolicyRegistrationTypes.TYPES.forEach(t -> verify(typeManager).registerTypes(t));
    }

    @Test
    void verifyDefaultPrivateKeyParserIsRegistered() {
        var privateKeyResolver = context.getPrivateKeyResolver();

        extension.initialize(context);

        verify(privateKeyResolver).addParser(eq(PrivateKey.class), any(DefaultPrivateKeyParseFunction.class));
    }
}
