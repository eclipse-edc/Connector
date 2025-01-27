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

package org.eclipse.edc.protocol.dsp.catalog.http.api.decorator;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromCriterionTransformer;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromQuerySpecTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToCriterionTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToQuerySpecTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_FILTER_EXPRESSION;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_LIMIT;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_OFFSET;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_SORT_FIELD;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_SORT_ORDER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Base64ContinuationTokenSerDesTest {

    private final TypeManager typeManager = mock();
    private final TypeTransformerRegistryImpl typeTransformerRegistry = new TypeTransformerRegistryImpl();
    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final Base64continuationTokenSerDes serDes = new Base64continuationTokenSerDes(typeTransformerRegistry, new TitaniumJsonLd(mock()));

    @BeforeEach
    void setup() {
        var builderFactory = Json.createBuilderFactory(emptyMap());
        typeTransformerRegistry.register(new JsonObjectFromQuerySpecTransformer(builderFactory));
        typeTransformerRegistry.register(new JsonObjectFromCriterionTransformer(builderFactory, typeManager, "test"));
        typeTransformerRegistry.register(new JsonValueToGenericTypeTransformer(typeManager, "test"));
        typeTransformerRegistry.register(new JsonObjectToQuerySpecTransformer());
        typeTransformerRegistry.register(new JsonObjectToCriterionTransformer());
        when(typeManager.getMapper("test")).thenReturn(objectMapper);
    }

    @Test
    void shouldSerializeAsBase64andDeserializeAsExpandedJsonLd() {
        var original = QuerySpec.Builder.newInstance()
                .limit(12)
                .sortField("any")
                .filter(List.of(Criterion.criterion("any", "any", "any")))
                .sortOrder(SortOrder.DESC)
                .offset(42)
                .build();

        assertThat(serDes.serialize(original)).isSucceeded()
                .matches(Base64::isBase64)
                .satisfies(serialized -> {
                    assertThat(serDes.deserialize(serialized)).isSucceeded().satisfies(jsonObject -> {
                        assertThat(jsonObject.getJsonArray(EDC_QUERY_SPEC_LIMIT).getJsonObject(0).getInt(VALUE)).isEqualTo(12);
                        assertThat(jsonObject.getJsonArray(EDC_QUERY_SPEC_SORT_FIELD).getJsonObject(0).getString(VALUE)).isEqualTo("any");
                        assertThat(jsonObject.getJsonArray(EDC_QUERY_SPEC_SORT_ORDER).getJsonObject(0).getString(VALUE)).isEqualTo("DESC");
                        assertThat(jsonObject.getJsonArray(EDC_QUERY_SPEC_OFFSET).getJsonObject(0).getInt(VALUE)).isEqualTo(42);
                        assertThat(jsonObject.getJsonArray(EDC_QUERY_SPEC_FILTER_EXPRESSION)).hasSize(1);
                    });
                });
    }

    @Test
    void deserialize_shouldFail_whenBodyNotBase64() {
        var result = serDes.deserialize("not-base-64");

        assertThat(result).isFailed();
    }
}
