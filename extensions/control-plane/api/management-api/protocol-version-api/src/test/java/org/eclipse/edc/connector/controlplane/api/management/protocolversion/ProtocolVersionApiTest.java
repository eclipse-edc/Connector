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

package org.eclipse.edc.connector.controlplane.api.management.protocolversion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.api.management.protocolversion.transform.JsonObjectToProtocolVersionRequestTransformer;
import org.eclipse.edc.connector.controlplane.api.management.protocolversion.validation.ProtocolVersionRequestValidator;
import org.eclipse.edc.connector.controlplane.protocolversion.spi.ProtocolVersionRequest;
import org.eclipse.edc.jsonld.JsonLdExtension;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.edc.connector.controlplane.api.management.protocolversion.v4alpha.ProtocolVersionApiV4alpha.ProtocolVersionRequestSchema.PROTOCOL_VERSION_REQUEST_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.api.management.protocolversion.v4alpha.ProtocolVersionApiV4alpha.ProtocolVersionSchema.PROTOCOL_VERSION_EXAMPLE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.junit.extensions.TestServiceExtensionContext.testServiceExtensionContext;

class ProtocolVersionApiTest {

    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final TypeTransformerRegistry transformer = new TypeTransformerRegistryImpl();
    private final JsonLd jsonLd = new JsonLdExtension().createJsonLdService(testServiceExtensionContext());

    @BeforeEach
    void setUp() {
        transformer.register(new JsonObjectToProtocolVersionRequestTransformer());
    }

    @Test
    void protocolVersionsRequestExample() throws JsonProcessingException {
        var validator = ProtocolVersionRequestValidator.instance();

        var jsonObject = objectMapper.readValue(PROTOCOL_VERSION_REQUEST_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        assertThat(expanded).isSucceeded()
                .satisfies(exp -> assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, ProtocolVersionRequest.class))
                .satisfies(transformResult -> assertThat(transformResult).isSucceeded()
                        .satisfies(transformed -> {
                            assertThat(transformed.getProtocol()).isNotBlank();
                            assertThat(transformed.getCounterPartyAddress()).isNotBlank();
                            assertThat(transformed.getCounterPartyId()).isNotBlank();
                        }));
    }

    @Test
    void protocolVersionsResponseExample() throws JsonProcessingException {
        var jsonObject = objectMapper.readValue(PROTOCOL_VERSION_EXAMPLE, JsonObject.class);

        assertThat(jsonObject.getJsonArray("protocolVersions"))
                .isNotNull()
                .extracting(List::size)
                .isEqualTo(2);

    }

}
