/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.port.transformer;

import org.eclipse.edc.signaling.domain.DataFlowResponseMessage;
import org.eclipse.edc.signaling.domain.DspDataAddress;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataFlowResponseMessageToDataFlowResponseTransformerTest {

    private final DataFlowResponseMessageToDataFlowResponseTransformer transformer = new DataFlowResponseMessageToDataFlowResponseTransformer();
    private final TransformerContext context = mock();

    @Test
    void shouldTransform() {
        var dataAddress = DataAddress.Builder.newInstance().type("any").build();
        when(context.transform(isA(DspDataAddress.class), any())).thenReturn(dataAddress);
        var message = DataFlowResponseMessage.Builder.newInstance()
                .dataplaneId("dataPlaneId")
                .dataAddress(DspDataAddress.Builder.newInstance().build())
                .state("STARTED")
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getDataPlaneId()).isSameAs("dataPlaneId");
        assertThat(result.getDataAddress()).isSameAs(dataAddress);
        assertThat(result.isAsync()).isFalse();
    }

    @Test
    void shouldBeAsync_whenStateEndsWithIng() {
        var dataAddress = DataAddress.Builder.newInstance().type("any").build();
        when(context.transform(isA(DspDataAddress.class), any())).thenReturn(dataAddress);
        var message = DataFlowResponseMessage.Builder.newInstance()
                .dataplaneId("dataPlaneId")
                .dataAddress(DspDataAddress.Builder.newInstance().build())
                .state("STARTING")
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.isAsync()).isTrue();
    }
}
