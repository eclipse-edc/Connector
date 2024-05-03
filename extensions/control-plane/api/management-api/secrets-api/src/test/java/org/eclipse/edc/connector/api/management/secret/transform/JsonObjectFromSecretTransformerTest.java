/*
 *  Copyright (c) 2024 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.secret.transform;


import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.spi.types.domain.secret.Secret;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_TYPE;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_VALUE;
import static org.mockito.Mockito.mock;


class JsonObjectFromSecretTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock();

    private final JsonObjectFromSecretTransformer transformer = new JsonObjectFromSecretTransformer(jsonFactory);

    @Test
    void transform_returnJsonObject() {
        var secret = Secret.Builder.newInstance()
                .id("test-secret-id")
                .value("my-test-value")
                .build();
        var result = transformer.transform(secret, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(ID).getString()).isEqualTo(secret.getId());
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(EDC_SECRET_TYPE);
        assertThat(result.getJsonString(EDC_SECRET_VALUE).getString()).isEqualTo(secret.getValue());
    }

}
