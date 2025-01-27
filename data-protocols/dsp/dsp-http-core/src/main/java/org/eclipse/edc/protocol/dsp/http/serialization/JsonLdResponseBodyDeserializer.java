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

package org.eclipse.edc.protocol.dsp.http.serialization;

import jakarta.json.JsonObject;
import okhttp3.ResponseBody;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.dsp.http.spi.dispatcher.response.DspHttpResponseBodyExtractor;
import org.eclipse.edc.protocol.dsp.spi.transform.DspProtocolTypeTransformerRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Failure;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.Function;

import static java.lang.String.format;
import static java.lang.String.join;

/**
 * Extract a Json-LD represented body from {@link ResponseBody}
 */
public class JsonLdResponseBodyDeserializer<T> implements DspHttpResponseBodyExtractor<T> {
    private final Class<T> type;
    private final TypeManager typeManager;
    private final String typeContext;
    private final JsonLd jsonLd;
    private final DspProtocolTypeTransformerRegistry dspTransformerRegistry;

    public JsonLdResponseBodyDeserializer(Class<T> type, TypeManager typeManager, String typeContext, JsonLd jsonLd, DspProtocolTypeTransformerRegistry dspTransformerRegistry) {
        this.type = type;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
        this.jsonLd = jsonLd;
        this.dspTransformerRegistry = dspTransformerRegistry;
    }

    @Override
    public T extractBody(ResponseBody responseBody, String protocol) {
        try {
            var jsonObject = typeManager.getMapper(typeContext).readValue(responseBody.byteStream(), JsonObject.class);
            var transformerRegistryResult = dspTransformerRegistry.forProtocol(protocol);
            if (transformerRegistryResult.failed()) {
                throw new EdcException(format("Failed to extract body: %s", join(", ", transformerRegistryResult.getFailureMessages())));
            }

            var registry = transformerRegistryResult.getContent();

            var expanded = jsonLd.expand(jsonObject).orElseThrow(exception("Cannot expand json-ld"));
            return registry.transform(expanded, type)
                    .orElseThrow(exception("Cannot transform json to ContractNegotiationAck"));

        } catch (IOException e) {
            throw new EdcException("Cannot deserialize response body as JsonObject", e);
        }
    }

    @NotNull
    private Function<Failure, EdcException> exception(String message) {
        return f -> new EdcException("%s: %s".formatted(message, f.getFailureDetail()));
    }

}
