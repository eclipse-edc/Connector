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

package org.eclipse.edc.protocol.dsp.catalog.transform.from;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Distribution;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DISTRIBUTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectFromDatasetTransformerTest {

    private static final String DATASET_PROPERTY = "catalog:prop:key";
    private static final String OFFER_ID = "offerId";

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private ObjectMapper mapper = mock(ObjectMapper.class);
    private TransformerContext context = mock(TransformerContext.class);

    private JsonObjectFromDatasetTransformer transformer;

    private JsonObject policyJson;
    private JsonObject distributionJson;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromDatasetTransformer(jsonFactory, mapper);

        policyJson = getJsonObject("policy");
        distributionJson = getJsonObject("distribution");

        when(context.transform(isA(Policy.class), eq(JsonObject.class))).thenReturn(policyJson);
        when(context.transform(isA(Distribution.class), eq(JsonObject.class))).thenReturn(distributionJson);
        when(context.problem()).thenReturn(new ProblemBuilder(context));
    }

    @Test
    void transform_returnJsonObject() {
        when(mapper.convertValue(any(), eq(JsonValue.class))).thenReturn(Json.createValue("value"));

        var dataset = getDataset();
        var result = transformer.transform(dataset, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(ID).getString()).isEqualTo(dataset.getId());
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(DCAT_DATASET_TYPE);

        assertThat(result.get(DCAT_DISTRIBUTION_ATTRIBUTE))
                .isNotNull()
                .isInstanceOf(JsonArray.class)
                .matches(v -> v.asJsonArray().size() == 1)
                .matches(v -> v.asJsonArray().get(0).equals(distributionJson));

        assertThat(result.get(ODRL_POLICY_ATTRIBUTE))
                .isNotNull()
                .isInstanceOf(JsonArray.class)
                .matches(v -> v.asJsonArray().size() == 1);
        // assert that original offer id has been appended to transformed policy
        var policyResult = result.get(ODRL_POLICY_ATTRIBUTE).asJsonArray().get(0);
        assertThat(policyResult.asJsonObject().getJsonString(ID).getString())
                .isNotNull()
                .isEqualTo(OFFER_ID);

        assertThat(result.get(DATASET_PROPERTY)).isNotNull();

        verify(context, times(1)).transform(dataset.getOffers().get(OFFER_ID), JsonObject.class);
        verify(context, times(1)).transform(dataset.getDistributions().get(0), JsonObject.class);
    }

    @Test
    void transform_mappingPropertyFails_reportProblem() {
        when(mapper.convertValue(any(), eq(JsonValue.class))).thenThrow(IllegalArgumentException.class);

        var dataset = getDataset();
        var result = transformer.transform(dataset, context);

        assertThat(result).isNotNull();
        assertThat(result.get(DATASET_PROPERTY)).isNull();

        verify(context, times(1)).reportProblem(anyString());
    }

    private Dataset getDataset() {
        return Dataset.Builder.newInstance()
                .id("dataset")
                .offer(OFFER_ID, Policy.Builder.newInstance().build())
                .distribution(Distribution.Builder.newInstance()
                        .format("format")
                        .dataService(DataService.Builder.newInstance().build())
                        .build())
                .property(DATASET_PROPERTY, "value")
                .build();
    }

    private JsonObject getJsonObject(String type) {
        return jsonFactory.createObjectBuilder()
                .add(TYPE, type)
                .build();
    }
}
