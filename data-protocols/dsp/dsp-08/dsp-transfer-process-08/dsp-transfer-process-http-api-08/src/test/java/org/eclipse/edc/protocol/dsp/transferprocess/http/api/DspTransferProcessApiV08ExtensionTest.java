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

package org.eclipse.edc.protocol.dsp.transferprocess.http.api;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants.DSP_NAMESPACE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class DspTransferProcessApiV08ExtensionTest {

    private final JsonObjectValidatorRegistry validatorRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(JsonObjectValidatorRegistry.class, validatorRegistry);
    }

    @Test
    void shouldRegisterMessageValidators(DspTransferProcessApiV08Extension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(validatorRegistry).register(eq(DSP_NAMESPACE_V_08.toIri(DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM)), any());
        verify(validatorRegistry).register(eq(DSP_NAMESPACE_V_08.toIri(DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM)), any());
        verify(validatorRegistry).register(eq(DSP_NAMESPACE_V_08.toIri(DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM)), any());
        verify(validatorRegistry).register(eq(DSP_NAMESPACE_V_08.toIri(DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM)), any());
    }

}
