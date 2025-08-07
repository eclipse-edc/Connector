/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.transform.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_PROPERTY_FILTER_TERM;

/**
 * Transforms a {@link JsonObject} in JSON-LD expanded form to a {@link CatalogRequestMessage}.
 */
public class JsonObjectToCatalogRequestMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, CatalogRequestMessage> {

    public JsonObjectToCatalogRequestMessageTransformer(JsonLdNamespace namespace) {
        super(JsonObject.class, CatalogRequestMessage.class, namespace);
    }

    @Override
    public @Nullable CatalogRequestMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = CatalogRequestMessage.Builder.newInstance();

        Optional.of(object)
                .map(it -> it.get(forNamespace(DSPACE_PROPERTY_FILTER_TERM)))
                .map(it -> transformObject(it, QuerySpec.class, context))
                .ifPresent(builder::querySpec);

        return builder.build();
    }

}
