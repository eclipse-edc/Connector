/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.transform.from;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import org.eclipse.edc.iam.decentralizedclaims.spi.model.PresentationQueryMessage;
import org.eclipse.edc.iam.decentralizedclaims.transform.TestContextProvider;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.decentralizedclaims.spi.model.PresentationQueryMessage.PRESENTATION_QUERY_MESSAGE_DEFINITION_TERM;
import static org.eclipse.edc.iam.decentralizedclaims.spi.model.PresentationQueryMessage.PRESENTATION_QUERY_MESSAGE_SCOPE_TERM;
import static org.eclipse.edc.iam.decentralizedclaims.spi.model.PresentationQueryMessage.PRESENTATION_QUERY_MESSAGE_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonObjectFromPresentationQueryTransformerTest {

    private final TransformerContext context = mock();
    private final ObjectMapper mapper = JacksonJsonLd.createObjectMapper();
    private final TypeManager typeManager = mock();

    @BeforeEach
    void setUp() {
        when(typeManager.getMapper("test")).thenReturn(mapper);
    }

    @ParameterizedTest
    @ArgumentsSource(TestContextProvider.class)
    void transform(TestContextProvider.TestContext ctx) {

        var transformer = new JsonObjectFromPresentationQueryTransformer(typeManager, "test", ctx.namespace());
        var response = PresentationQueryMessage.Builder.newinstance().scopes(List.of("scope")).build();

        var json = transformer.transform(response, context);

        assertThat(json).isNotNull();
        assertThat(json.getJsonString(TYPE).getString()).isEqualTo(ctx.toIri(PRESENTATION_QUERY_MESSAGE_TERM));
        assertThat(json.getJsonArray(ctx.toIri(PRESENTATION_QUERY_MESSAGE_SCOPE_TERM)))
                .hasSize(1)
                .contains(Json.createValue("scope"));

    }

    @ParameterizedTest
    @ArgumentsSource(TestContextProvider.class)
    void transform_withPresentationDefinition(TestContextProvider.TestContext ctx) {

        var transformer = new JsonObjectFromPresentationQueryTransformer(typeManager, "test", ctx.namespace());
        var response = PresentationQueryMessage.Builder.newinstance()
                .presentationDefinition(PresentationDefinition.Builder.newInstance()
                        .id("id")
                        .build()).build();

        var json = transformer.transform(response, context);

        assertThat(json).isNotNull();
        assertThat(json.getJsonString(TYPE).getString()).isEqualTo(ctx.toIri(PRESENTATION_QUERY_MESSAGE_TERM));
        assertThat(json.getJsonObject(ctx.toIri(PRESENTATION_QUERY_MESSAGE_DEFINITION_TERM)))
                .isNotNull();

    }
}
