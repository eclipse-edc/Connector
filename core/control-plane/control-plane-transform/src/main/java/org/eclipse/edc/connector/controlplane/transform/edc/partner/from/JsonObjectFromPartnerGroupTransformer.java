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

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdFromModelTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup.EDC_PARTNER_GROUP_DESCRIPTION_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup.EDC_PARTNER_GROUP_NAME_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup.EDC_PARTNER_GROUP_PROPERTIES_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup.EDC_PARTNER_GROUP_TYPE_IRI;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;

/**
 * Converts a {@link PartnerGroup} into an expanded JSON-LD {@link JsonObject}.
 */
public class JsonObjectFromPartnerGroupTransformer extends JsonLdFromModelTransformer<PartnerGroup, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromPartnerGroupTransformer(JsonBuilderFactory jsonFactory) {
        super(PartnerGroup.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull PartnerGroup group, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder()
                .add(ID, group.getId())
                .add(TYPE, EDC_PARTNER_GROUP_TYPE_IRI)
                .add(EDC_PARTNER_GROUP_NAME_IRI, group.getName())
                .add(EDC_PARTNER_GROUP_PROPERTIES_IRI, createProperties(group));
        if (group.getDescription() != null) {
            builder.add(EDC_PARTNER_GROUP_DESCRIPTION_IRI, group.getDescription());
        }
        return builder.build();
    }

    private JsonObject createProperties(PartnerGroup group) {
        return jsonFactory.createObjectBuilder()
                .add(VALUE, jsonFactory.createObjectBuilder(group.getProperties()))
                .add(TYPE, JSON)
                .build();
    }
}
