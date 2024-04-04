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
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Distribution;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_ACCESS_SERVICE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DISTRIBUTION_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectFromDistributionTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock();

    private final JsonObjectFromDistributionTransformer transformer = new JsonObjectFromDistributionTransformer(jsonFactory);

    @Test
    void transform_returnJsonObject() {
        var accessServiceJson = jsonFactory.createObjectBuilder().add(ID, "dataServiceId").build();
        when(context.transform(any(), eq(JsonObject.class))).thenReturn(accessServiceJson);
        var distribution = Distribution.Builder.newInstance()
                .format("format")
                .dataService(DataService.Builder.newInstance().id("dataServiceId").build())
                .build();

        var result = transformer.transform(distribution, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(DCAT_DISTRIBUTION_TYPE);
        assertThat(result.getJsonObject(DCT_FORMAT_ATTRIBUTE).getJsonString(ID).getString())
                .isEqualTo(distribution.getFormat());
        assertThat(result.getJsonObject(DCAT_ACCESS_SERVICE_ATTRIBUTE).getString(ID)).isEqualTo("dataServiceId");
        verify(context).transform(isA(DataService.class), eq(JsonObject.class));
    }

}
