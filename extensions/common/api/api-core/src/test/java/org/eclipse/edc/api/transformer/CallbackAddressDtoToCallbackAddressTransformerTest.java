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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.api.transformer;

import org.eclipse.edc.api.model.CallbackAddressDto;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CallbackAddressDtoToCallbackAddressTransformerTest {

    private final CallbackAddressDtoToCallbackAddressTransformer transformer = new CallbackAddressDtoToCallbackAddressTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        var dto = CallbackAddressDto.Builder.newInstance()
                .uri("http://test")
                .events(Set.of("event"))
                .transactional(true)
                .build();

        var callback = transformer.transform(dto, context);

        assertThat(callback).usingRecursiveComparison().isEqualTo(dto);
    }

}
