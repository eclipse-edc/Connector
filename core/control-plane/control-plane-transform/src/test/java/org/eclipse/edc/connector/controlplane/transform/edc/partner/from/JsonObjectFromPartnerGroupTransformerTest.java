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
import org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup.EDC_PARTNER_GROUP_DESCRIPTION_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup.EDC_PARTNER_GROUP_NAME_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup.EDC_PARTNER_GROUP_PROPERTIES_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup.EDC_PARTNER_GROUP_TYPE_IRI;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.mockito.Mockito.mock;

class JsonObjectFromPartnerGroupTransformerTest {

    private final TransformerContext context = mock();
    private final JsonObjectFromPartnerGroupTransformer transformer = new JsonObjectFromPartnerGroupTransformer(Json.createBuilderFactory(Map.of()));

    @Test
    void transform_shouldConvertPartnerGroupToJsonObject() {
        var group = PartnerGroup.Builder.newInstance()
                .id("gold").participantContextId("pc").name("Gold tier").description("Premium")
                .properties(Map.of("region", "eu"))
                .build();

        var result = transformer.transform(group, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("gold");
        assertThat(result.getString(TYPE)).isEqualTo(EDC_PARTNER_GROUP_TYPE_IRI);
        assertThat(result.getString(EDC_PARTNER_GROUP_NAME_IRI)).isEqualTo("Gold tier");
        assertThat(result.getString(EDC_PARTNER_GROUP_DESCRIPTION_IRI)).isEqualTo("Premium");
        assertThat(result.getJsonObject(EDC_PARTNER_GROUP_PROPERTIES_IRI).getJsonObject(VALUE).getString("region")).isEqualTo("eu");
    }

    @Test
    void transform_shouldOmitDescription_whenNull() {
        var group = PartnerGroup.Builder.newInstance().id("gold").name("Gold tier").build();

        var result = transformer.transform(group, context);

        assertThat(result).isNotNull();
        assertThat(result.containsKey(EDC_PARTNER_GROUP_DESCRIPTION_IRI)).isFalse();
    }
}
