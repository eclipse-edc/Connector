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

package org.eclipse.edc.iam.identitytrust.transform.from;

import jakarta.json.Json;
import org.eclipse.edc.iam.identitytrust.spi.model.PresentationResponseMessage;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.model.PresentationResponseMessage.PRESENTATION_RESPONSE_MESSAGE_PRESENTATION_PROPERTY;
import static org.eclipse.edc.iam.identitytrust.spi.model.PresentationResponseMessage.PRESENTATION_RESPONSE_MESSAGE_TYPE_PROPERTY;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.mockito.Mockito.mock;

public class JsonObjectFromPresentationResponseMessageTransformerTest {

    private final JsonObjectFromPresentationResponseMessageTransformer transformer = new JsonObjectFromPresentationResponseMessageTransformer();

    private final TransformerContext context = mock();

    @Test
    void transform() {

        var response = PresentationResponseMessage.Builder.newinstance().presentation(List.of("jwt")).build();

        var json = transformer.transform(response, context);

        assertThat(json).isNotNull();
        assertThat(json.getJsonString(TYPE).getString()).isEqualTo(PRESENTATION_RESPONSE_MESSAGE_TYPE_PROPERTY);

        assertThat(json.getJsonObject(PRESENTATION_RESPONSE_MESSAGE_PRESENTATION_PROPERTY))
                .extracting(object -> object.get(VALUE).asJsonArray())
                .satisfies(arr -> {
                    assertThat(arr).hasSize(1)
                            .first()
                            .isEqualTo(Json.createValue("jwt"));
                });

    }

    @Test
    void transform_withJson() {
        var response = PresentationResponseMessage.Builder.newinstance()
                .presentation(List.of(Map.of("@context", List.of())))
                .build();

        var json = transformer.transform(response, context);

        assertThat(json).isNotNull();
        assertThat(json.getJsonString(TYPE).getString()).isEqualTo(PRESENTATION_RESPONSE_MESSAGE_TYPE_PROPERTY);

        var expected = Json.createObjectBuilder()
                .add("@context", Json.createArrayBuilder().build())
                .build();


        assertThat(json.getJsonObject(PRESENTATION_RESPONSE_MESSAGE_PRESENTATION_PROPERTY))
                .extracting(object -> object.get(VALUE).asJsonArray())
                .satisfies(arr -> {
                    assertThat(arr).hasSize(1)
                            .first()
                            .isEqualTo(expected);
                });

    }

    @Test
    void transform_withStringAndJson() {
        var response = PresentationResponseMessage.Builder.newinstance()
                .presentation(List.of("jwt", Map.of("@context", List.of())))
                .build();
        var json = transformer.transform(response, context);

        assertThat(json).isNotNull();
        assertThat(json.getJsonString(TYPE).getString()).isEqualTo(PRESENTATION_RESPONSE_MESSAGE_TYPE_PROPERTY);

        var complex = Json.createObjectBuilder()
                .add("@context", Json.createArrayBuilder().build())
                .build();

        assertThat(json.getJsonObject(PRESENTATION_RESPONSE_MESSAGE_PRESENTATION_PROPERTY))
                .extracting(object -> object.get(VALUE).asJsonArray())
                .satisfies(arr -> {
                    assertThat(arr).hasSize(2).containsExactly(Json.createValue("jwt"), complex);
                });

    }
}
