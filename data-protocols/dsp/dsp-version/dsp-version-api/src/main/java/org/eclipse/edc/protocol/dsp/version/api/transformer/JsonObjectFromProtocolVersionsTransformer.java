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

package org.eclipse.edc.protocol.dsp.version.api.transformer;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.spi.protocol.ProtocolVersions;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.protocol.dsp.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_PATH;
import static org.eclipse.edc.protocol.dsp.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_PROTOCOL_VERSIONS;
import static org.eclipse.edc.protocol.dsp.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_VERSION;

/**
 * Transform {@link ProtocolVersions} into {@link JsonObject}
 */
public class JsonObjectFromProtocolVersionsTransformer extends AbstractJsonLdTransformer<ProtocolVersions, JsonObject> {

    public JsonObjectFromProtocolVersionsTransformer() {
        super(ProtocolVersions.class, JsonObject.class);
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ProtocolVersions protocolVersions, @NotNull TransformerContext context) {
        var versions = protocolVersions.protocolVersions().stream()
                .map(version -> Json.createObjectBuilder()
                        .add(DSPACE_PROPERTY_VERSION, version.version())
                        .add(DSPACE_PROPERTY_PATH, version.path())
                        .build())
                .collect(toJsonArray());

        return Json.createObjectBuilder().add(DSPACE_PROPERTY_PROTOCOL_VERSIONS, versions).build();
    }
}
