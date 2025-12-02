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

package org.eclipse.edc.connector.controlplane.api.management.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.api.management.catalog.validation.CatalogRequestValidator;
import org.eclipse.edc.connector.controlplane.api.management.catalog.validation.DatasetRequestValidator;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest;
import org.eclipse.edc.connector.controlplane.transform.edc.catalog.to.JsonObjectToCatalogRequestTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.catalog.to.JsonObjectToDatasetRequestTransformer;
import org.eclipse.edc.jsonld.JsonLdExtension;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToQuerySpecTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.edc.connector.controlplane.api.management.catalog.v3.CatalogApiV3.CatalogRequestSchema.CATALOG_REQUEST_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.api.management.catalog.v3.CatalogApiV3.CatalogSchema.CATALOG_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.api.management.catalog.v3.CatalogApiV3.DatasetRequestSchema.DATASET_REQUEST_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.api.management.catalog.v3.CatalogApiV3.DatasetSchema.DATASET_EXAMPLE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_CATALOG_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_ATTRIBUTE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.junit.extensions.TestServiceExtensionContext.testServiceExtensionContext;
import static org.mockito.Mockito.mock;

class CatalogApiTest {

    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final JsonLd jsonLd = new JsonLdExtension().createJsonLdService(testServiceExtensionContext());
    private final TypeTransformerRegistry transformer = new TypeTransformerRegistryImpl();

    @BeforeEach
    void setUp() {
        transformer.register(new JsonObjectToCatalogRequestTransformer());
        transformer.register(new JsonObjectToDatasetRequestTransformer());
        transformer.register(new JsonObjectToQuerySpecTransformer());
    }

    @Test
    void catalogRequestExample() throws JsonProcessingException {
        var validator = CatalogRequestValidator.instance(mock());

        var jsonObject = objectMapper.readValue(CATALOG_REQUEST_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        assertThat(expanded).isSucceeded()
                .satisfies(exp -> assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, CatalogRequest.class))
                .satisfies(transformResult -> assertThat(transformResult).isSucceeded()
                        .satisfies(transformed -> {
                            assertThat(transformed.getProtocol()).isNotBlank();
                            assertThat(transformed.getCounterPartyAddress()).isNotBlank();
                            assertThat(transformed.getCounterPartyId()).isNotBlank();
                            assertThat(transformed.getQuerySpec()).isNotNull();
                        }));
    }

    @Test
    void datasetRequestExample() throws JsonProcessingException {
        var validator = DatasetRequestValidator.instance();

        var jsonObject = objectMapper.readValue(DATASET_REQUEST_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        assertThat(expanded).isSucceeded()
                .satisfies(exp -> assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, DatasetRequest.class))
                .satisfies(transformResult -> assertThat(transformResult).isSucceeded()
                        .satisfies(transformed -> {
                            assertThat(transformed.getProtocol()).isNotBlank();
                            assertThat(transformed.getCounterPartyId()).isNotBlank();
                            assertThat(transformed.getCounterPartyAddress()).isNotBlank();
                        }));
    }

    @Test
    void catalogExample() throws JsonProcessingException {
        var jsonObject = objectMapper.readValue(CATALOG_EXAMPLE, JsonObject.class);
        var expanded = jsonLd.expand(jsonObject);

        assertThat(expanded).isSucceeded().satisfies(content -> {
            assertThat(content.getString(ID)).isNotBlank();
            assertThat(content.getJsonArray(TYPE).getString(0)).isEqualTo(DCAT_CATALOG_TYPE);
            assertThat(content.getJsonArray(DCAT_DATASET_ATTRIBUTE).size()).isGreaterThan(0);
        });
    }

    @Test
    void datasetExample() throws JsonProcessingException {
        var jsonObject = objectMapper.readValue(DATASET_EXAMPLE, JsonObject.class);
        var expanded = jsonLd.expand(jsonObject);

        assertThat(expanded).isSucceeded().satisfies(content -> {
            assertThat(content.getString(ID)).isNotBlank();
            assertThat(content.getJsonArray(TYPE).getString(0)).isEqualTo(DCAT_DATASET_TYPE);
            assertThat(content.getJsonArray(ODRL_POLICY_ATTRIBUTE).size()).isGreaterThan(0);
        });
    }
}
