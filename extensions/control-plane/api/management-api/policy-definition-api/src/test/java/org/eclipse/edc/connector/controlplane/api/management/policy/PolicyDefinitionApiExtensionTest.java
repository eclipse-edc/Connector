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

package org.eclipse.edc.connector.controlplane.api.management.policy;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.controlplane.api.management.policy.v2.PolicyDefinitionApiV2Controller;
import org.eclipse.edc.connector.controlplane.api.management.policy.v3.PolicyDefinitionApiV3Controller;
import org.eclipse.edc.connector.controlplane.api.management.policy.v31alpha.PolicyDefinitionApiV31AlphaController;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class PolicyDefinitionApiExtensionTest {

    private final JsonObjectValidatorRegistry validatorRegistry = mock();
    private final WebService webService = mock();
    private PolicyDefinitionApiExtension extension;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        TypeTransformerRegistry typeTransformerRegistry = mock();
        when(typeTransformerRegistry.forContext(any())).thenReturn(mock());
        context.registerService(TypeTransformerRegistry.class, typeTransformerRegistry);
        context.registerService(JsonObjectValidatorRegistry.class, validatorRegistry);
        context.registerService(WebService.class, webService);
        extension = factory.constructInstance(PolicyDefinitionApiExtension.class);
    }

    @Test
    void initialize_shouldRegisterValidatorForPolicyDefinition(ServiceExtensionContext context) {
        extension.initialize(context);

        verify(validatorRegistry).register(eq(EDC_POLICY_DEFINITION_TYPE), any());
    }

    @Test
    void initialize_shouldRegisterControllers(ServiceExtensionContext context) {
        extension.initialize(context);

        verify(webService).registerResource(any(), isA(PolicyDefinitionApiV2Controller.class));
        verify(webService).registerResource(any(), isA(PolicyDefinitionApiV3Controller.class));
        verify(webService).registerResource(any(), isA(PolicyDefinitionApiV31AlphaController.class));
    }
}
