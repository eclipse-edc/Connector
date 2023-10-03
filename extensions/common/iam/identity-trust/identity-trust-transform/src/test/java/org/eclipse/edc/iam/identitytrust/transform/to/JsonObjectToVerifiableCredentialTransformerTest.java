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

package org.eclipse.edc.iam.identitytrust.transform.to;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.transform.TestConstants.EXAMPLE_VC_JSONLD;
import static org.mockito.Mockito.mock;

@Disabled("Not yet implemented fully")
class JsonObjectToVerifiableCredentialTransformerTest {
    public static final ObjectMapper OBJECT_MAPPER = JacksonJsonLd.createObjectMapper();
    private final TransformerContext context = mock();
    private final JsonLd jsonLdService = new TitaniumJsonLd(mock());
    private JsonObjectToVerifiableCredentialTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToVerifiableCredentialTransformer(OBJECT_MAPPER);
    }

    @Test
    void transform() throws JsonProcessingException {

        var jsonObj = OBJECT_MAPPER.readValue(EXAMPLE_VC_JSONLD, JsonObject.class);
        var vc = transformer.transform(jsonObj, context);

        assertThat(vc).isNotNull();
        assertThat(vc.getRawVc()).isNotNull().isEqualTo(EXAMPLE_VC_JSONLD);

    }
}