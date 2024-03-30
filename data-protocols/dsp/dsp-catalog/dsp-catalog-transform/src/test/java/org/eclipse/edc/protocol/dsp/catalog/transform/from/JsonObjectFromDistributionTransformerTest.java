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

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Distribution;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_ACCESS_SERVICE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DISTRIBUTION_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;
import static org.mockito.Mockito.mock;

class JsonObjectFromDistributionTransformerTest {

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private TransformerContext context = mock(TransformerContext.class);

    private JsonObjectFromDistributionTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromDistributionTransformer(jsonFactory);
    }

    @Test
    void transform_returnJsonObject() {
        var distribution = getDistribution();
        var result = transformer.transform(distribution, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(DCAT_DISTRIBUTION_TYPE);
        assertThat(result.getJsonObject(DCT_FORMAT_ATTRIBUTE).getJsonString(ID).getString())
                .isEqualTo(distribution.getFormat());
        assertThat(result.getJsonString(DCAT_ACCESS_SERVICE_ATTRIBUTE).getString())
                .isEqualTo(distribution.getDataService().getId());
    }

    private Distribution getDistribution() {
        return Distribution.Builder.newInstance()
                .format("format")
                .dataService(DataService.Builder.newInstance().id("dataServiceId").build())
                .build();
    }
}
