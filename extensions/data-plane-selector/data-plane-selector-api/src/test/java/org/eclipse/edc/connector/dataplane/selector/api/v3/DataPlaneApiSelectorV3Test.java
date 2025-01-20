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

package org.eclipse.edc.connector.dataplane.selector.api.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataPlaneInstanceTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.selector.api.v3.DataPlaneInstanceSchemaV3.DATAPLANE_INSTANCE_EXAMPLE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataPlaneApiSelectorV3Test {

    private final TypeManager typeManager = mock();
    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final JsonLd jsonLd = new TitaniumJsonLd(mock());
    private final TypeTransformerRegistry transformer = new TypeTransformerRegistryImpl();

    @BeforeEach
    void setUp() {
        transformer.register(new JsonObjectToDataPlaneInstanceTransformer());
        transformer.register(new JsonObjectToDataAddressTransformer());
        transformer.register(new JsonValueToGenericTypeTransformer(typeManager, "test"));
        when(typeManager.getMapper("test")).thenReturn(objectMapper);
    }

    @Test
    void dataPlaneInstanceInputExample() throws JsonProcessingException {

        var jsonObject = objectMapper.readValue(DATAPLANE_INSTANCE_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        AbstractResultAssert.assertThat(expanded).isSucceeded()
                .extracting(e -> transformer.transform(e, DataPlaneInstance.class).getContent())
                .isNotNull()
                .satisfies(transformed -> {
                    assertThat(transformed.getId()).isNotBlank();
                    assertThat(transformed.getUrl().toString()).isEqualTo("http://somewhere.com:1234/api/v1");
                    assertThat(transformed.getAllowedDestTypes()).containsExactlyInAnyOrder("your-dest-type");
                    assertThat(transformed.getAllowedSourceTypes()).containsExactlyInAnyOrder("source-type1", "source-type2");
                    assertThat(transformed.getAllowedTransferTypes()).containsExactlyInAnyOrder("transfer-type");
                });
    }

}
