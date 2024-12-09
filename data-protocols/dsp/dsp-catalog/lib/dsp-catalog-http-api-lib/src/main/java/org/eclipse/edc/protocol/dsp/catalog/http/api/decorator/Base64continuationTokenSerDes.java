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

package org.eclipse.edc.protocol.dsp.catalog.http.api.decorator;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.dsp.http.spi.message.ContinuationTokenSerDes;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public class Base64continuationTokenSerDes implements ContinuationTokenSerDes {

    private final TypeTransformerRegistry typeTransformerRegistry;
    private final JsonLd jsonLd;

    public Base64continuationTokenSerDes(TypeTransformerRegistry typeTransformerRegistry, JsonLd jsonLd) {
        this.typeTransformerRegistry = typeTransformerRegistry;
        this.jsonLd = jsonLd;
    }

    @Override
    public Result<String> serialize(QuerySpec querySpec) {
        return typeTransformerRegistry.transform(querySpec, JsonObject.class)
                .map(Object::toString)
                .map(String::getBytes)
                .map(Base64.getEncoder()::encodeToString);
    }

    @Override
    public Result<JsonObject> deserialize(String serialized) {
        try {
            var decode = Base64.getDecoder().decode(serialized);
            var jsonObject = Json.createReader(new ByteArrayInputStream(decode)).readObject();
            return jsonLd.expand(jsonObject);
        } catch (Exception e) {
            return Result.failure("Cannot deserialize continuationToken: " + e.getMessage());
        }

    }
}
