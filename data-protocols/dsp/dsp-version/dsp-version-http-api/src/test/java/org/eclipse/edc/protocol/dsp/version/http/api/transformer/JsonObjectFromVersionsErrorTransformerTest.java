/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.version.http.api.transformer;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionsError;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON_TERM;
import static org.mockito.Mockito.mock;

class JsonObjectFromVersionsErrorTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectFromVersionsError transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromVersionsError(jsonFactory);
    }

    @Test
    void transform_returnJsonObject() {

        var error = VersionsError.Builder.newInstance()
                .code("code")
                .messages(List.of("message"))
                .build();

        var result = transformer.transform(error, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(DSPACE_PROPERTY_CODE_TERM)).isEqualTo("code");
        assertThat(result.getJsonArray(DSPACE_PROPERTY_REASON_TERM)).contains(Json.createValue("message"));
    }
}
