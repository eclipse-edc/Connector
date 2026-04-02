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

import org.eclipse.edc.signaling.domain.DspDataAddress;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.signaling.domain.DspDataAddress.DSP_DATA_ADDRESS_ENDPOINT;
import static org.mockito.Mockito.mock;

class DspDataAddressToDataAddressTransformerTest {

    private final DspDataAddressToDataAddressTransformer transformer = new DspDataAddressToDataAddressTransformer();

    @Test
    void shouldTransform() {
        var dspDataAddress = DspDataAddress.Builder.newInstance().endpoint("endpoint").endpointType("type")
                .property("key", "value").build();

        var result = transformer.transform(dspDataAddress, mock());

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo("type");
        assertThat(result.getProperty(DSP_DATA_ADDRESS_ENDPOINT)).isEqualTo("endpoint");
        assertThat(result.getProperty("key")).isEqualTo("value");
    }
}
