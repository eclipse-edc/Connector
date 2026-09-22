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
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.partner.spi.Partner;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdFromModelTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_GROUP_IDS_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_IDENTITY_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_NAME_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_PROPERTIES_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_TYPE_IRI;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;

/**
 * Converts a {@link Partner} into an expanded JSON-LD {@link JsonObject}.
 */
public class JsonObjectFromPartnerTransformer extends JsonLdFromModelTransformer<Partner, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromPartnerTransformer(JsonBuilderFactory jsonFactory) {
        super(Partner.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull Partner partner, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder()
                .add(ID, partner.getId())
                .add(TYPE, EDC_PARTNER_TYPE_IRI)
                .add(EDC_PARTNER_IDENTITY_IRI, partner.getIdentity())
                .add(EDC_PARTNER_PROPERTIES_IRI, createProperties(partner))
                .add(EDC_PARTNER_GROUP_IDS_IRI, Json.createArrayBuilder(partner.getGroupIds()));
        if (partner.getName() != null) {
            builder.add(EDC_PARTNER_NAME_IRI, partner.getName());
        }
        return builder.build();
    }

    private JsonObject createProperties(Partner partner) {
        return jsonFactory.createObjectBuilder()
                .add(VALUE, jsonFactory.createObjectBuilder(partner.getProperties()))
                .add(TYPE, JSON)
                .build();
    }
}
