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

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation;

import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v3.ContractNegotiationApiV3Controller;
import org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.from.JsonObjectFromContractNegotiationTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.from.JsonObjectFromNegotiationStateTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.to.JsonObjectToContractOfferTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.to.JsonObjectToTerminateNegotiationCommandTransformer;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand.TERMINATE_NEGOTIATION_TYPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class ContractNegotiationApiExtensionTest {

    private final JsonObjectValidatorRegistry validatorRegistry = mock(JsonObjectValidatorRegistry.class);
    private final WebService webService = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        when(typeTransformerRegistry.forContext(any())).thenReturn(mock());
        context.registerService(TypeTransformerRegistry.class, typeTransformerRegistry);
        context.registerService(JsonObjectValidatorRegistry.class, validatorRegistry);
        context.registerService(WebService.class, webService);
    }

    @Test
    void initiate_shouldRegisterValidators(ServiceExtensionContext context, ContractNegotiationApiExtension extension) {
        extension.initialize(context);

        verify(validatorRegistry).register(eq(CONTRACT_REQUEST_TYPE), any());
        verify(validatorRegistry).register(eq(TERMINATE_NEGOTIATION_TYPE), any());
    }

    @Test
    void initiate_shouldRegisterControllers(ServiceExtensionContext context, ContractNegotiationApiExtension extension) {
        extension.initialize(context);

        verify(webService).registerResource(any(), isA(ContractNegotiationApiV3Controller.class));
    }

    @Test
    void initiate_shouldRegisterTransformers(ServiceExtensionContext context, ContractNegotiationApiExtension extension) {
        var scopedRegistry = mock(TypeTransformerRegistry.class);
        when(typeTransformerRegistry.forContext(eq("management-api"))).thenReturn(scopedRegistry);
        extension.initialize(context);

        verify(scopedRegistry).register(isA(JsonObjectToContractOfferTransformer.class));
        verify(scopedRegistry).register(isA(JsonObjectToTerminateNegotiationCommandTransformer.class));
        verify(scopedRegistry).register(isA(JsonObjectFromContractNegotiationTransformer.class));
        verify(scopedRegistry).register(isA(JsonObjectFromNegotiationStateTransformer.class));
    }
}
