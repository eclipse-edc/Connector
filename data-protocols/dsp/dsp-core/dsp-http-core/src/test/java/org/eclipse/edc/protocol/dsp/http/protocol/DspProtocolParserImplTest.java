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

package org.eclipse.edc.protocol.dsp.http.protocol;

import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersionRegistry;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersions;
import org.eclipse.edc.protocol.dsp.spi.version.DspVersions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP_V_2024_1;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DspProtocolParserImplTest {

    private final ProtocolVersionRegistry protocolVersionRegistry = mock();
    private final DspProtocolParserImpl parser = new DspProtocolParserImpl(protocolVersionRegistry);
    private final ProtocolVersions protocolVersions = new ProtocolVersions(List.of(DspVersions.V_08, DspVersions.V_2024_1));

    @BeforeEach
    void beforeEach() {
        when(protocolVersionRegistry.getAll()).thenReturn(protocolVersions);
    }

    @Test
    void shouldParseProtocol() {
        assertThat(parser.parse(DATASPACE_PROTOCOL_HTTP)).isSucceeded()
                .isEqualTo(DspVersions.V_08);
    }

    @Test
    void shouldParseProtocolWithVersion() {
        assertThat(parser.parse(DATASPACE_PROTOCOL_HTTP_V_2024_1)).isSucceeded()
                .isEqualTo(DspVersions.V_2024_1);
    }

    @Test
    void shouldFailToParseProtocol_whenTypeNotSupported() {
        assertThat(parser.parse("myprotocol:v10")).isFailed();
    }

    @Test
    void shouldFailToParseProtocol_whenVersionNotSupported() {
        assertThat(parser.parse(DATASPACE_PROTOCOL_HTTP + ":wrongVersion")).isFailed();
    }
}
