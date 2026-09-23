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
import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_GROUP_IDS_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_IDENTITY_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_NAME_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_PROPERTIES_IRI;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToPartnerTransformerTest {

    private final TransformerContext context = mock();
    private final JsonObjectToPartnerTransformer transformer = new JsonObjectToPartnerTransformer();

    @Test
    void transform_shouldConvertJsonObjectToPartner() {
        when(context.transform(any(), any())).thenAnswer(a -> a.getArgument(0) instanceof JsonString js ? js.getString() : a.getArgument(0));
        var json = Json.createObjectBuilder()
                .add(ID, "partner-id")
                .add(EDC_PARTNER_IDENTITY_IRI, "did:web:acme")
                .add(EDC_PARTNER_NAME_IRI, "ACME")
                .add(EDC_PARTNER_PROPERTIES_IRI, Json.createObjectBuilder()
                        .add(VALUE, Json.createObjectBuilder().add("businessId", "BID-0001"))
                        .add(TYPE, JSON))
                .add(EDC_PARTNER_GROUP_IDS_IRI, Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add(VALUE, "gold"))
                        .add(Json.createObjectBuilder().add(VALUE, "eu")))
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("partner-id");
        assertThat(result.getIdentity()).isEqualTo("did:web:acme");
        assertThat(result.getName()).isEqualTo("ACME");
        assertThat(result.getProperties()).containsEntry("businessId", "BID-0001");
        assertThat(result.getGroupIds()).containsExactlyInAnyOrder("gold", "eu");
        assertThat(result.getParticipantContextId()).isNull();
    }

    @Test
    void transform_shouldGenerateId_andDefaultCollections() {
        var json = Json.createObjectBuilder()
                .add(EDC_PARTNER_IDENTITY_IRI, "did:web:acme")
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotBlank();
        assertThat(result.getProperties()).isEmpty();
        assertThat(result.getGroupIds()).isEmpty();
    }

    @Test
    void transform_shouldReportProblem_whenPropertiesNotAnObject() {
        var json = Json.createObjectBuilder()
                .add(EDC_PARTNER_IDENTITY_IRI, "did:web:acme")
                .add(EDC_PARTNER_PROPERTIES_IRI, Json.createObjectBuilder().add(VALUE, "text").add(TYPE, JSON))
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNull();
        verify(context).reportProblem(any());
    }
}
