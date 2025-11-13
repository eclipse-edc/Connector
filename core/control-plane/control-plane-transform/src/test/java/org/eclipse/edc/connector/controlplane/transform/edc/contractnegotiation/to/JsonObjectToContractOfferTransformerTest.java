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

package org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.to;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_SET;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_TARGET_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToContractOfferTransformerTest {

    private final JsonLd jsonLd = new TitaniumJsonLd(mock(Monitor.class));
    private final TransformerContext context = mock();
    private JsonObjectToContractOfferTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToContractOfferTransformer();
    }

    @Test
    void transform() {
        var offerPolicy = createObjectBuilder()
                .add(TYPE, ODRL_POLICY_TYPE_SET)
                .add(ID, "test-offer-id")
                .add(ODRL_TARGET_ATTRIBUTE, "test-asset")
                .add(ODRL_PERMISSION_ATTRIBUTE, getJsonObject("permission"))
                .add(ODRL_PROHIBITION_ATTRIBUTE, getJsonObject("prohibition"))
                .add(ODRL_OBLIGATION_ATTRIBUTE, getJsonObject("duty"))
                .build();

        var policy = Policy.Builder.newInstance().target("test-asset").build();
        when(context.transform(any(JsonValue.class), eq(Policy.class))).thenReturn(policy);

        var result = transformer.transform(jsonLd.expand(offerPolicy).getContent(), context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("test-offer-id");
        assertThat(result.getAssetId()).isEqualTo("test-asset");
    }

    @Test
    void shouldReturnNull_whenPolicyIsNull() {
        when(context.transform(any(JsonValue.class), eq(Policy.class))).thenReturn(null);

        var result = transformer.transform(createObjectBuilder().build(), context);

        assertThat(result).isNull();
    }

    private JsonObject getJsonObject(String type) {
        return createObjectBuilder()
                .add(TYPE, type)
                .build();
    }
}
