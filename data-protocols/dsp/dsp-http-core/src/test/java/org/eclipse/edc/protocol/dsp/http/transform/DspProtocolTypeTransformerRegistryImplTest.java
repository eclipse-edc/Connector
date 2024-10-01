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

package org.eclipse.edc.protocol.dsp.http.transform;

import org.eclipse.edc.protocol.dsp.http.spi.DspProtocolParser;
import org.eclipse.edc.protocol.dsp.spi.version.DspVersions;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT_V_08;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DspProtocolTypeTransformerRegistryImplTest {

    private final TypeTransformerRegistry transformerRegistry = mock();
    private final DspProtocolParser protocolParser = mock();
    private final DspProtocolTypeTransformerRegistryImpl dspTransformerRegistry = new DspProtocolTypeTransformerRegistryImpl(transformerRegistry, DSP_TRANSFORMER_CONTEXT, protocolParser);

    @Test
    void forProtocol() {
        when(protocolParser.parse(DATASPACE_PROTOCOL_HTTP)).thenReturn(Result.success(DspVersions.V_08));
        when(transformerRegistry.forContext(DSP_TRANSFORMER_CONTEXT_V_08)).thenReturn(transformerRegistry);
        assertThat(dspTransformerRegistry.forProtocol(DATASPACE_PROTOCOL_HTTP)).isSucceeded()
                .isEqualTo(transformerRegistry);
    }

}
