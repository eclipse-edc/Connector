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
import org.eclipse.edc.iam.identitytrust.transform.TestContextProvider;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.model.PresentationResponseMessage.PRESENTATION_RESPONSE_MESSAGE_PRESENTATION_TERM;
import static org.eclipse.edc.iam.identitytrust.spi.model.PresentationResponseMessage.PRESENTATION_RESPONSE_MESSAGE_TYPE_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.mockito.Mockito.mock;

public class JsonObjectFromPresentationResponseMessageTransformerTest {

    private final TransformerContext context = mock();

    @ParameterizedTest
    @ArgumentsSource(TestContextProvider.class)
    void transform(TestContextProvider.TestContext ctx) {

        var transformer = new JsonObjectFromPresentationResponseMessageTransformer(ctx.namespace());
        var response = PresentationResponseMessage.Builder.newinstance().presentation(List.of("jwt")).build();

        var json = transformer.transform(response, context);

        assertThat(json).isNotNull();
        assertThat(json.getJsonString(TYPE).getString()).isEqualTo(ctx.toIri(PRESENTATION_RESPONSE_MESSAGE_TYPE_TERM));

        assertThat(json.getJsonObject(ctx.toIri(PRESENTATION_RESPONSE_MESSAGE_PRESENTATION_TERM)))
                .extracting(object -> object.get(VALUE).asJsonArray())
                .satisfies(arr -> {
                    assertThat(arr).hasSize(1)
                            .first()
                            .isEqualTo(Json.createValue("jwt"));
                });

    }

    @ParameterizedTest
    @ArgumentsSource(TestContextProvider.class)
    void transform_withJson(TestContextProvider.TestContext ctx) {
        var transformer = new JsonObjectFromPresentationResponseMessageTransformer(ctx.namespace());

        var response = PresentationResponseMessage.Builder.newinstance()
                .presentation(List.of(Map.of("@context", List.of())))
                .build();

        var json = transformer.transform(response, context);

        assertThat(json).isNotNull();
        assertThat(json.getJsonString(TYPE).getString()).isEqualTo(ctx.toIri(PRESENTATION_RESPONSE_MESSAGE_TYPE_TERM));

        var expected = Json.createObjectBuilder()
                .add("@context", Json.createArrayBuilder().build())
                .build();


        assertThat(json.getJsonObject(ctx.toIri(PRESENTATION_RESPONSE_MESSAGE_PRESENTATION_TERM)))
                .extracting(object -> object.get(VALUE).asJsonArray())
                .satisfies(arr -> {
                    assertThat(arr).hasSize(1)
                            .first()
                            .isEqualTo(expected);
                });

    }

    @ParameterizedTest
    @ArgumentsSource(TestContextProvider.class)
    void transform_withStringAndJson(TestContextProvider.TestContext ctx) {
        var transformer = new JsonObjectFromPresentationResponseMessageTransformer(ctx.namespace());

        var response = PresentationResponseMessage.Builder.newinstance()
                .presentation(List.of("jwt", Map.of("@context", List.of())))
                .build();
        var json = transformer.transform(response, context);

        assertThat(json).isNotNull();
        assertThat(json.getJsonString(TYPE).getString()).isEqualTo(ctx.toIri(PRESENTATION_RESPONSE_MESSAGE_TYPE_TERM));

        var complex = Json.createObjectBuilder()
                .add("@context", Json.createArrayBuilder().build())
                .build();

        assertThat(json.getJsonObject(ctx.toIri(PRESENTATION_RESPONSE_MESSAGE_PRESENTATION_TERM)))
                .extracting(object -> object.get(VALUE).asJsonArray())
                .satisfies(arr -> {
                    assertThat(arr).hasSize(2).containsExactly(Json.createValue("jwt"), complex);
                });

    }
}
