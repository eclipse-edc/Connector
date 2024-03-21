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

package org.eclipse.edc.protocol.dsp.version.api.transformer;

import jakarta.json.JsonValue;
import org.eclipse.edc.connector.spi.protocol.ProtocolVersion;
import org.eclipse.edc.connector.spi.protocol.ProtocolVersions;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_PATH;
import static org.eclipse.edc.protocol.dsp.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_PROTOCOL_VERSIONS;
import static org.eclipse.edc.protocol.dsp.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_VERSION;
import static org.mockito.Mockito.mock;

class JsonObjectFromProtocolVersionsTransformerTest {

    private final TransformerContext context = mock();
    private final JsonObjectFromProtocolVersionsTransformer transformer =
            new JsonObjectFromProtocolVersionsTransformer();

    @Test
    void shouldTransform() {
        var protocolVersion = new ProtocolVersion("version", "/path");
        var protocolVersions = new ProtocolVersions(List.of(protocolVersion));

        var result = transformer.transform(protocolVersions, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonArray(DSPACE_PROPERTY_PROTOCOL_VERSIONS)).isNotEmpty()
                .hasSize(1).first().extracting(JsonValue::asJsonObject).satisfies(version -> {
                    assertThat(version.getString(DSPACE_PROPERTY_VERSION)).isEqualTo("version");
                    assertThat(version.getString(DSPACE_PROPERTY_PATH)).isEqualTo("/path");
                });
    }
}
