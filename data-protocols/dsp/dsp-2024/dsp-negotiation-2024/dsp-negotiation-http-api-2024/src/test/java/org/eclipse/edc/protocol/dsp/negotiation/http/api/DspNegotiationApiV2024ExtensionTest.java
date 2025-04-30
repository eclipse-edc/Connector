/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.http.api;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_IRI;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class DspNegotiationApiV2024ExtensionTest {

    private final JsonObjectValidatorRegistry validatorRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(JsonObjectValidatorRegistry.class, validatorRegistry);
    }

    @Test
    void shouldRegisterMessageValidators(DspNegotiationApiV2024Extension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(validatorRegistry).register(eq(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_IRI), any());
        verify(validatorRegistry).register(eq(DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_IRI), any());
        verify(validatorRegistry).register(eq(DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_IRI), any());
        verify(validatorRegistry).register(eq(DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_IRI), any());
        verify(validatorRegistry).register(eq(DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_IRI), any());
        verify(validatorRegistry).register(eq(DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_IRI), any());
    }
}
