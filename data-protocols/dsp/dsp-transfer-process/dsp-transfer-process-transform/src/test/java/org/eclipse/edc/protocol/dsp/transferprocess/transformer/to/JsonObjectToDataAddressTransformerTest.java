/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.transformer.to;

import jakarta.json.Json;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class JsonObjectToDataAddressTransformerTest {

    private final String type = "testType";

    private final String key = "testKey";

    private final String propertyKey = "region";

    private final String propertyValue = "europe";

    private TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToDataAddressTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToDataAddressTransformer();
    }

    @Test
    void jsonObjectToTransferCompletionMessage() {

        var json = Json.createObjectBuilder()
                .add("type", type)
                .add("keyName", key)
                .add(propertyKey, propertyValue)
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();

        assertThat(result.getType()).isEqualTo(type);
        assertThat(result.getKeyName()).isEqualTo(key);
        assertThat(result.getProperty(propertyKey)).isEqualTo(propertyValue);

        verify(context, never()).reportProblem(anyString());
    }
}
