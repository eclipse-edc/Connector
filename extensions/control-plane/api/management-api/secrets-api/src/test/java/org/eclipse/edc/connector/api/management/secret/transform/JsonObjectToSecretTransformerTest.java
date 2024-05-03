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
 *       Amadeus - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.api.management.secret.transform;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_TYPE;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_VALUE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class JsonObjectToSecretTransformerTest {

    private static final String TEST_SECRET_ID = "secret-id";
    private static final String TEST_SECRET_VALUE = "secret-value";

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock();

    private final JsonObjectToSecretTransformer transformer = new JsonObjectToSecretTransformer();

    @Test
    void transform_returnSecret() {
        var secret = jsonFactory.createObjectBuilder()
                .add(ID, TEST_SECRET_ID)
                .add(TYPE, EDC_SECRET_TYPE)
                .add(EDC_SECRET_VALUE, TEST_SECRET_VALUE)
                .build();

        var result = transformer.transform(secret, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_SECRET_ID);
        assertThat(result.getValue()).isEqualTo(TEST_SECRET_VALUE);

        verifyNoInteractions(context);
    }

    @Test
    void transform_withMissingValueInJsonObject_throwsException() {
        var secret = jsonFactory.createObjectBuilder()
                .add(ID, TEST_SECRET_ID)
                .add(TYPE, EDC_SECRET_TYPE)
                .build();

        assertThatNullPointerException().isThrownBy(() -> transformer.transform(secret, context));
    }

}


