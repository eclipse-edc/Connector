/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.transform.v2025.from;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Distribution;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_CATALOG_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_CATALOG_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATA_SERVICE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DISTRIBUTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DSPACE_PROPERTY_PARTICIPANT_ID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_NAMESPACE_V_2025_1;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectFromCatalogV2025TransformerTest {

    private static final String CATALOG_PROPERTY = "catalog:prop:key";

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final ObjectMapper mapper = mock();
    private final TransformerContext context = mock();
    private final ParticipantIdMapper participantIdMapper = mock();
    private final TypeManager typeManager = mock();

    private final JsonObjectFromCatalogV2025Transformer transformer = new JsonObjectFromCatalogV2025Transformer(jsonFactory, typeManager, "test", participantIdMapper);

    private JsonObject datasetJson;
    private JsonObject catalogJson;
    private JsonObject dataServiceJson;

    @BeforeEach
    void setUp() {
        when(typeManager.getMapper("test")).thenReturn(mapper);
        datasetJson = getJsonObject("dataset");
        catalogJson = getJsonObject("Catalog");
        dataServiceJson = getJsonObject("dataService");

        when(context.transform(isA(Dataset.class), eq(JsonObject.class))).thenReturn(datasetJson);
        when(context.transform(isA(Catalog.class), eq(JsonObject.class))).thenReturn(catalogJson);
        when(context.transform(isA(DataService.class), eq(JsonObject.class))).thenReturn(dataServiceJson);
        when(context.problem()).thenReturn(new ProblemBuilder(context));
        when(participantIdMapper.toIri(any())).thenReturn("urn:namespace:participantId");
    }

    @Test
    void transform_returnJsonObject() {
        when(mapper.convertValue(any(), eq(JsonValue.class))).thenReturn(Json.createValue("value"));
        var catalog = getCatalog();

        var result = transformer.transform(catalog, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(ID).getString()).isEqualTo(catalog.getId());
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(DCAT_CATALOG_TYPE);

        assertThat(result.get(DCAT_DATASET_ATTRIBUTE))
                .isNotNull()
                .isInstanceOf(JsonArray.class)
                .matches(v -> v.asJsonArray().size() == 1)
                .matches(v -> v.asJsonArray().get(0).equals(datasetJson));

        assertThat(result.get(DCAT_DATA_SERVICE_ATTRIBUTE))
                .isNotNull()
                .isInstanceOf(JsonArray.class)
                .matches(v -> v.asJsonArray().size() == 1)
                .matches(v -> v.asJsonArray().get(0).equals(dataServiceJson));
        assertThat(result.getJsonObject(DSP_NAMESPACE_V_2025_1.toIri(DSPACE_PROPERTY_PARTICIPANT_ID_TERM)).getString(ID)).isEqualTo("urn:namespace:participantId");
        assertThat(result.get(CATALOG_PROPERTY)).isNotNull();

        verify(context, times(1)).transform(catalog.getDatasets().get(0), JsonObject.class);
        verify(context, times(1)).transform(catalog.getDataServices().get(0), JsonObject.class);
    }

    @Test
    void transform_SubCatalogs_returnJsonObject() {
        when(mapper.convertValue(any(), eq(JsonValue.class))).thenReturn(Json.createValue("value"));
        var catalog = getCatalogWithSubCatalog();

        var result = transformer.transform(catalog, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(ID).getString()).isEqualTo(catalog.getId());
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(DCAT_CATALOG_TYPE);

        assertThat(result.get(DCAT_DATASET_ATTRIBUTE))
                .isNotNull()
                .isInstanceOf(JsonArray.class)
                .matches(v -> v.asJsonArray().size() == 1)
                .matches(v -> v.asJsonArray().get(0).equals(datasetJson));

        assertThat(result.get(DCAT_CATALOG_ATTRIBUTE))
                .isNotNull()
                .isInstanceOf(JsonArray.class)
                .matches(v -> v.asJsonArray().size() == 1)
                .matches(v -> v.asJsonArray().get(0).equals(catalogJson));

        assertThat(result.get(DCAT_DATA_SERVICE_ATTRIBUTE))
                .isNotNull()
                .isInstanceOf(JsonArray.class)
                .matches(v -> v.asJsonArray().size() == 1)
                .matches(v -> v.asJsonArray().get(0).equals(dataServiceJson));
        assertThat(result.getJsonObject(DSP_NAMESPACE_V_2025_1.toIri(DSPACE_PROPERTY_PARTICIPANT_ID_TERM)).getString(ID)).isEqualTo("urn:namespace:participantId");
        assertThat(result.get(CATALOG_PROPERTY)).isNotNull();

        verify(context, times(1)).transform(catalog.getDatasets().get(0), JsonObject.class);
        verify(context, times(1)).transform(catalog.getDataServices().get(0), JsonObject.class);
    }

    @Test
    void transform_emptyProperties() {
        when(mapper.convertValue(any(), eq(JsonValue.class))).thenReturn(Json.createValue("value"));
        var catalog = Catalog.Builder.newInstance().participantId("participantId").build();

        var result = transformer.transform(catalog, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(ID).getString()).isEqualTo(catalog.getId());
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(DCAT_CATALOG_TYPE);

        assertThat(result.get(DCAT_DATASET_ATTRIBUTE)).isNull();
        assertThat(result.get(DCAT_CATALOG_ATTRIBUTE)).isNull();
        assertThat(result.get(DCAT_DATA_SERVICE_ATTRIBUTE)).isNull();
        assertThat(result.get(DCAT_DISTRIBUTION_ATTRIBUTE)).isNull();
        assertThat(result.getJsonObject(DSP_NAMESPACE_V_2025_1.toIri(DSPACE_PROPERTY_PARTICIPANT_ID_TERM)).getString(ID)).isEqualTo("urn:namespace:participantId");

    }

    @Test
    void transform_mappingPropertyFails_reportProblem() {
        when(mapper.convertValue(any(), eq(JsonValue.class))).thenThrow(IllegalArgumentException.class);

        var catalog = getCatalog();
        var result = transformer.transform(catalog, context);

        assertThat(result).isNotNull();
        assertThat(result.get(CATALOG_PROPERTY)).isNull();

        verify(context, times(1)).reportProblem(anyString());
    }

    private Catalog getCatalog() {
        return getCatalogBuilder().build();
    }

    private Catalog getCatalogWithSubCatalog() {
        return getCatalogBuilder()
                .dataset(Catalog.Builder.newInstance().build()).build();
    }

    private Catalog.Builder getCatalogBuilder() {
        return Catalog.Builder.newInstance()
                .id("catalog")
                .dataset(Dataset.Builder.newInstance()
                        .offer("offerId", Policy.Builder.newInstance().build())
                        .distribution(Distribution.Builder.newInstance()
                                .format("format")
                                .dataService(DataService.Builder.newInstance().build())
                                .build())
                        .build())
                .dataService(DataService.Builder.newInstance().build())
                .participantId("participantId")
                .property(CATALOG_PROPERTY, "value");
    }

    private JsonObject getJsonObject(String type) {
        return jsonFactory.createObjectBuilder()
                .add(TYPE, type)
                .build();
    }
}
