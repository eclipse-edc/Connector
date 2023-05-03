/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.api.transformer;

import jakarta.json.Json;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.api.model.CriterionDto.CRITERION_TYPE;
import static org.eclipse.edc.api.model.CriterionDto.from;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;
import static org.mockito.Mockito.mock;

class JsonObjectFromCriterionDtoTransformerTest {

    private final JsonObjectFromCriterionDtoTransformer transformer = new JsonObjectFromCriterionDtoTransformer(Json.createBuilderFactory(Map.of()), createObjectMapper());
    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void transform() {
        var criterion = from("foo", "=", "bar");
        var json = transformer.transform(criterion, context);

        assertThat(json).isNotNull();
        assertThat(json.getJsonString(TYPE).getString()).isEqualTo(CRITERION_TYPE);
    }

}