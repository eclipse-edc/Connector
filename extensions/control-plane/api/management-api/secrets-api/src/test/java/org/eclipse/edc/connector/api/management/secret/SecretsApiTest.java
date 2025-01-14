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

package org.eclipse.edc.connector.api.management.secret;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.management.secret.transform.JsonObjectToSecretTransformer;
import org.eclipse.edc.connector.api.management.secret.validation.SecretsValidator;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.domain.secret.Secret;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.edc.connector.api.management.secret.v3.SecretsApiV3.SecretInputSchema.SECRET_INPUT_EXAMPLE;
import static org.eclipse.edc.connector.api.management.secret.v3.SecretsApiV3.SecretOutputSchema.SECRET_OUTPUT_EXAMPLE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_TYPE;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_VALUE;
import static org.mockito.Mockito.mock;

class SecretsApiTest {
    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final JsonLd jsonLd = new TitaniumJsonLd(mock());
    private final TypeTransformerRegistry transformer = new TypeTransformerRegistryImpl();

    @BeforeEach
    void setUp() {
        transformer.register(new JsonObjectToSecretTransformer());
        transformer.register(new JsonValueToGenericTypeTransformer(objectMapper));
    }

    @Test
    void secretInputExample() throws JsonProcessingException {
        var validator = SecretsValidator.instance();

        var jsonObject = objectMapper.readValue(SECRET_INPUT_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        assertThat(expanded).isSucceeded()
                .satisfies(exp -> assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, Secret.class).getContent())
                .isNotNull()
                .satisfies(transformed -> {
                    assertThat(transformed.getId()).isNotBlank();
                    assertThat(transformed.getValue()).isNotBlank();
                });
    }

    @Test
    void secretOutputExample() throws JsonProcessingException {
        var jsonObject = objectMapper.readValue(SECRET_OUTPUT_EXAMPLE, JsonObject.class);
        var expanded = jsonLd.expand(jsonObject);

        assertThat(expanded).isSucceeded().satisfies(content -> {
            assertThat(content.getString(ID)).isNotBlank();

            assertThat(content.getJsonArray(TYPE).getString(0)).isEqualTo(EDC_SECRET_TYPE);
            assertThat(content.getJsonArray(EDC_SECRET_VALUE).getJsonObject(0).getString(VALUE)).isNotBlank();
        });
    }
}
