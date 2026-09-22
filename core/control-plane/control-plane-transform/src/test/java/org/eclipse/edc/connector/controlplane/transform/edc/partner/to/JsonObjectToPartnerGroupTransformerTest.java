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

package org.eclipse.edc.connector.controlplane.transform.edc.partner.to;

import jakarta.json.Json;
import jakarta.json.JsonString;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup.EDC_PARTNER_GROUP_DESCRIPTION_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup.EDC_PARTNER_GROUP_NAME_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup.EDC_PARTNER_GROUP_PROPERTIES_IRI;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToPartnerGroupTransformerTest {

    private final TransformerContext context = mock();
    private final JsonObjectToPartnerGroupTransformer transformer = new JsonObjectToPartnerGroupTransformer();

    @Test
    void transform_shouldConvertJsonObjectToPartnerGroup() {
        when(context.transform(any(), any())).thenAnswer(a -> a.getArgument(0) instanceof JsonString js ? js.getString() : a.getArgument(0));
        var json = Json.createObjectBuilder()
                .add(ID, "gold")
                .add(EDC_PARTNER_GROUP_NAME_IRI, "Gold tier")
                .add(EDC_PARTNER_GROUP_DESCRIPTION_IRI, "Premium partners")
                .add(EDC_PARTNER_GROUP_PROPERTIES_IRI, Json.createObjectBuilder()
                        .add(VALUE, Json.createObjectBuilder().add("region", "eu"))
                        .add(TYPE, JSON))
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("gold");
        assertThat(result.getName()).isEqualTo("Gold tier");
        assertThat(result.getDescription()).isEqualTo("Premium partners");
        assertThat(result.getProperties()).containsEntry("region", "eu");
    }

    @Test
    void transform_shouldGenerateId() {
        var json = Json.createObjectBuilder().add(EDC_PARTNER_GROUP_NAME_IRI, "Gold tier").build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotBlank();
        assertThat(result.getDescription()).isNull();
        assertThat(result.getProperties()).isEmpty();
    }
}
