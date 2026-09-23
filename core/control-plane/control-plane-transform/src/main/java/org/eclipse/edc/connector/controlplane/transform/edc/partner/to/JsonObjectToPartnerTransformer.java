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
import org.eclipse.edc.connector.controlplane.partner.spi.Partner;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdToModelTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_GROUP_IDS_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_IDENTITY_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_NAME_IRI;
import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_PROPERTIES_IRI;

/**
 * Converts an expanded JSON-LD {@link JsonObject} into a {@link Partner}. The participant context id is never read
 * from the payload, it is assigned by the caller.
 */
public class JsonObjectToPartnerTransformer extends JsonLdToModelTransformer<JsonObject, Partner> {

    public JsonObjectToPartnerTransformer() {
        super(JsonObject.class, Partner.class);
    }

    @Override
    public @Nullable Partner transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = Partner.Builder.newInstance();
        var nodeId = nodeId(object);
        builder.id(nodeId != null ? nodeId : UUID.randomUUID().toString());

        transformString(object.get(EDC_PARTNER_IDENTITY_IRI), builder::identity, context);
        transformString(object.get(EDC_PARTNER_NAME_IRI), builder::name, context);

        var properties = object.get(EDC_PARTNER_PROPERTIES_IRI);
        if (properties != null) {
            var jsonValue = nodeJsonValue(properties);
            if (jsonValue instanceof JsonObject json) {
                visitProperties(json, (key, value) -> builder.property(key, transformGenericProperty(value, context)));
            } else {
                context.reportProblem("Expected properties to be a JsonObject");
                return null;
            }
        }

        var groupIds = new HashSet<String>();
        ofNullable(object.getJsonArray(EDC_PARTNER_GROUP_IDS_IRI))
                .ifPresent(array -> groupIds.addAll(array.stream().map(this::nodeValue).toList()));
        builder.groupIds(groupIds);

        return builder.build();
    }
}
