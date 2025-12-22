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

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.signaling.domain.DspDataAddress.DSP_DATA_ADDRESS_ENDPOINT;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.mockito.Mockito.mock;

class DataAddressToDspDataAddressTransformerTest {

    private final DataAddressToDspDataAddressTransformer transformer = new DataAddressToDspDataAddressTransformer();

    @Test
    void shouldTransform() {
        var dataAddress = DataAddress.Builder.newInstance()
                .type("type")
                .property(DSP_DATA_ADDRESS_ENDPOINT, "endpoint")
                .property(EDC_NAMESPACE + "additionalProperty", "value")
                .build();

        var result = transformer.transform(dataAddress, mock());

        assertThat(result).isNotNull();
        assertThat(result.getEndpointType()).isEqualTo("type");
        assertThat(result.getEndpoint()).isEqualTo("endpoint");
        assertThat(result.getEndpointProperties()).first().satisfies(property -> {
            assertThat(property.getName()).isEqualTo(EDC_NAMESPACE + "additionalProperty");
            assertThat(property.getValue()).isEqualTo("value");
        });
    }
}
