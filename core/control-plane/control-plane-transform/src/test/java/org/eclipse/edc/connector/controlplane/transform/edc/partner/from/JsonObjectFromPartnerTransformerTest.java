/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.controlplane.transform.edc.partner.from;

import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.partner.spi.Partner;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_GROUP_IDS_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_IDENTITY_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_NAME_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_PROPERTIES_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_TYPE_IRI;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.mockito.Mockito.mock;

class JsonObjectFromPartnerTransformerTest {

    private final TransformerContext context = mock();
    private final JsonObjectFromPartnerTransformer transformer = new JsonObjectFromPartnerTransformer(Json.createBuilderFactory(Map.of()));

    @Test
    void transform_shouldConvertPartnerToJsonObject() {
        var partner = Partner.Builder.newInstance()
                .id("partner-id").participantContextId("pc").identity("did:web:acme").name("ACME")
                .properties(Map.of("businessId", "BID-0001"))
                .groupIds(Set.of("gold", "eu"))
                .build();

        var result = transformer.transform(partner, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("partner-id");
        assertThat(result.getString(TYPE)).isEqualTo(EDC_PARTNER_TYPE_IRI);
        assertThat(result.getString(EDC_PARTNER_IDENTITY_IRI)).isEqualTo("did:web:acme");
        assertThat(result.getString(EDC_PARTNER_NAME_IRI)).isEqualTo("ACME");
        var properties = result.getJsonObject(EDC_PARTNER_PROPERTIES_IRI);
        assertThat(properties.getString(TYPE)).isEqualTo(JSON);
        assertThat(properties.getJsonObject(VALUE).getString("businessId")).isEqualTo("BID-0001");
        assertThat(result.getJsonArray(EDC_PARTNER_GROUP_IDS_IRI))
                .containsExactlyInAnyOrder(Json.createValue("gold"), Json.createValue("eu"));
        assertThat(result.containsKey("participantContextId")).isFalse();
    }

    @Test
    void transform_shouldOmitName_whenNull() {
        var partner = Partner.Builder.newInstance().id("partner-id").identity("did:web:acme").build();

        var result = transformer.transform(partner, context);

        assertThat(result).isNotNull();
        assertThat(result.containsKey(EDC_PARTNER_NAME_IRI)).isFalse();
        assertThat(result.getJsonArray(EDC_PARTNER_GROUP_IDS_IRI)).isEmpty();
    }
}
