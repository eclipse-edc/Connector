/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.management.asset.transform;

import org.eclipse.edc.connector.api.management.asset.model.DataAddressDto;
import org.eclipse.edc.spi.dataaddress.DataAddressValidator;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataAddressDtoToDataAddressTransformerTest {

    private final DataAddressValidator validator = mock(DataAddressValidator.class);
    private final DataAddressDtoToDataAddressTransformer transformer = new DataAddressDtoToDataAddressTransformer(validator);

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void shouldTransform() {
        when(validator.validate(any())).thenAnswer(i -> Result.success(i.getArgument(0)));
        var context = mock(TransformerContext.class);
        var dataAddressDto = DataAddressDto.Builder.newInstance()
                .properties(Map.of("type", "any"))
                .build();

        var dataAddress = transformer.transform(dataAddressDto, context);

        assertThat(dataAddress.getProperties()).containsExactlyEntriesOf(dataAddressDto.getProperties());
    }

    @Test
    void shouldReturnNull_whenValidationFails() {
        when(validator.validate(any())).thenReturn(Result.failure(List.of("an error", "another error")));
        var context = mock(TransformerContext.class);
        var dataAddressDto = DataAddressDto.Builder.newInstance()
                .properties(Map.of("type", "any"))
                .build();

        var dataAddress = transformer.transform(dataAddressDto, context);

        assertThat(dataAddress).isNull();
        verify(context).reportProblem("an error");
        verify(context).reportProblem("another error");
    }
}
