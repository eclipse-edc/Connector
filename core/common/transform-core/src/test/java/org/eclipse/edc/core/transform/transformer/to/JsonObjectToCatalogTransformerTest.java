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

package org.eclipse.edc.core.transform.transformer.to;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.catalog.spi.DataService;
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.catalog.spi.Distribution;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.core.transform.transformer.TestInput.getExpanded;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_CATALOG_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATA_SERVICE_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JsonObjectToCatalogTransformerTest {

    private static final String CATALOG_ID = "catalogId";

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToCatalogTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToCatalogTransformer();
    }

    @Test
    void transform_emptyCatalog_returnCatalog() {
        var catalog = jsonFactory.createObjectBuilder().add(ID, CATALOG_ID).add(TYPE, DCAT_CATALOG_TYPE).build();

        var result = transformer.transform(getExpanded(catalog), context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(CATALOG_ID);
        assertThat(result.getDatasets()).isNull();
        assertThat(result.getDataServices()).isNull();

        verifyNoInteractions(context);
    }

    @Test
    void transform_catalogWithAdditionalProperty_returnCatalog() {
        var propertyKey = "catalog:prop:key";
        var propertyValue = "value";

        when(context.transform(any(JsonValue.class), eq(Object.class))).thenReturn(propertyValue);

        var catalog = jsonFactory.createObjectBuilder()
                .add(ID, CATALOG_ID)
                .add(TYPE, DCAT_CATALOG_TYPE)
                .add(propertyKey, propertyValue)
                .build();

        var result = transformer.transform(getExpanded(catalog), context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(CATALOG_ID);
        assertThat(result.getProperties()).hasSize(1);
        assertThat(result.getProperties()).containsEntry(propertyKey, propertyValue);

        verify(context, never()).reportProblem(anyString());
        verify(context, times(1)).transform(any(JsonValue.class), eq(Object.class));
    }

    @Test
    void transform_filledCatalog_returnCatalog() {
        var dataSetJson = getJsonObject("dataset");
        var dataServiceJson = getJsonObject("dataService");

        var dataset = Dataset.Builder.newInstance()
                .offer("offerId", Policy.Builder.newInstance().build())
                .distribution(Distribution.Builder.newInstance()
                        .format("format")
                        .dataService(DataService.Builder.newInstance().build())
                        .build())
                .build();
        var dataService = DataService.Builder.newInstance().build();

        when(context.transform(any(JsonObject.class), eq(Dataset.class)))
                .thenReturn(dataset);
        when(context.transform(any(JsonObject.class), eq(DataService.class)))
                .thenReturn(dataService);

        var catalog = jsonFactory.createObjectBuilder()
                .add(ID, CATALOG_ID)
                .add(TYPE, DCAT_CATALOG_TYPE)
                .add(DCAT_DATASET_ATTRIBUTE, dataSetJson)
                .add(DCAT_DATA_SERVICE_ATTRIBUTE, dataServiceJson)
                .build();

        var result = transformer.transform(getExpanded(catalog), context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(CATALOG_ID);
        assertThat(result.getDatasets()).hasSize(1);
        assertThat(result.getDatasets().get(0)).isEqualTo(dataset);
        assertThat(result.getDataServices()).hasSize(1);
        assertThat(result.getDataServices().get(0)).isEqualTo(dataService);

        verify(context, never()).reportProblem(anyString());
        verify(context, times(1)).transform(isA(JsonObject.class), eq(Dataset.class));
        verify(context, times(1)).transform(isA(JsonObject.class), eq(DataService.class));
    }

    @Test
    void transform_invalidType_reportProblem() {
        var catalog = jsonFactory.createObjectBuilder().add(TYPE, "not-a-catalog").build();

        transformer.transform(getExpanded(catalog), context);

        verify(context, never()).reportProblem(anyString());
    }

    private JsonObject getJsonObject(String type) {
        return jsonFactory.createObjectBuilder().add(TYPE, type).build();
    }

}
