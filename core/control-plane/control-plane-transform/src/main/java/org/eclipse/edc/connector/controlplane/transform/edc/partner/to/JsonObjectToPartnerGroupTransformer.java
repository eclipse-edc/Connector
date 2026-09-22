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

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdToModelTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup.EDC_PARTNER_GROUP_DESCRIPTION_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup.EDC_PARTNER_GROUP_NAME_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup.EDC_PARTNER_GROUP_PROPERTIES_IRI;

/**
 * Converts an expanded JSON-LD {@link JsonObject} into a {@link PartnerGroup}. The participant context id is never
 * read from the payload, it is assigned by the caller.
 */
public class JsonObjectToPartnerGroupTransformer extends JsonLdToModelTransformer<JsonObject, PartnerGroup> {

    public JsonObjectToPartnerGroupTransformer() {
        super(JsonObject.class, PartnerGroup.class);
    }

    @Override
    public @Nullable PartnerGroup transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = PartnerGroup.Builder.newInstance();
        var nodeId = nodeId(object);
        builder.id(nodeId != null ? nodeId : UUID.randomUUID().toString());

        transformString(object.get(EDC_PARTNER_GROUP_NAME_IRI), builder::name, context);
        transformString(object.get(EDC_PARTNER_GROUP_DESCRIPTION_IRI), builder::description, context);

        var properties = object.get(EDC_PARTNER_GROUP_PROPERTIES_IRI);
        if (properties != null) {
            var jsonValue = nodeJsonValue(properties);
            if (jsonValue instanceof JsonObject json) {
                visitProperties(json, (key, value) -> builder.property(key, transformGenericProperty(value, context)));
            } else {
                context.reportProblem("Expected properties to be a JsonObject");
                return null;
            }
        }

        return builder.build();
    }
}
