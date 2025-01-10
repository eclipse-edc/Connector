/*
 *  Copyright (c) 2024 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.api.management.secret;

import org.eclipse.edc.connector.api.management.secret.transform.JsonObjectFromSecretTransformer;
import org.eclipse.edc.connector.api.management.secret.transform.JsonObjectToSecretTransformer;
import org.eclipse.edc.connector.api.management.secret.v3.SecretsApiV3Controller;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class SecretsApiExtensionTest {

    private final TypeTransformerRegistry managementApiTypeTransformerRegistry = mock();
    private final JsonObjectValidatorRegistry validatorRegistry = mock();
    private final WebService webService = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        var typeTransformerRegistry = mock(TypeTransformerRegistry.class);
        when(typeTransformerRegistry.forContext("management-api")).thenReturn(managementApiTypeTransformerRegistry);
        context.registerService(TypeTransformerRegistry.class, typeTransformerRegistry);
        context.registerService(JsonObjectValidatorRegistry.class, validatorRegistry);
        context.registerService(WebService.class, webService);
    }

    @Test
    void initialize_shouldRegisterControllers(SecretsApiExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(webService).registerResource(any(), isA(SecretsApiV3Controller.class));
    }

    @Test
    void initialize_shouldRegisterValidators(SecretsApiExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(validatorRegistry).register(eq(EDC_SECRET_TYPE), any());
    }

    @Test
    void initialize_shouldRegisterTransformers(SecretsApiExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(managementApiTypeTransformerRegistry).register(any(JsonObjectToSecretTransformer.class));
        verify(managementApiTypeTransformerRegistry).register(any(JsonObjectFromSecretTransformer.class));
    }
}
