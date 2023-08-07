/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.security.signature.jws2020;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.ld.DocumentError;
import com.apicatalog.ld.signature.SigningError;
import com.apicatalog.vc.processor.Issuer;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.util.reflection.ReflectionUtil;

import java.net.URI;
import java.util.Arrays;

/**
 * The {@link Issuer} adds the context, but currently that adds hard-coded {@code "https://w3id.org/security/suites/ed25519-2020/v1"}.
 * For the Jwk2020 suite we need that to be {@code "https://w3id.org/security/suites/jws-2020/v1"}, so as a temporary workaround we do <em>not</em>
 * use {@link Issuer#getCompacted()}, but rather use {@link IssuerCompat#compact(Issuer, String...)}.
 */
public class IssuerCompat {
    /**
     * Compacts the JSON structure represented  by the {@link Issuer} by delegating to {@link JsonLd#compact(Document, URI)}. Note that before compacting, the JSON-LD is expanded, signed, all additional contexts are added
     * and then compacted.
     * <p>
     * By default, the following contexts are added automatically:
     * <ul>
     *     <li>https://www.w3.org/2018/credentials/v1</li>
     *     <li>https://w3id.org/security/suites/jws-2020/v1</li>
     * </ul>
     *
     * @param issuer             The {@link Issuer}
     * @param additionalContexts Any additional context URIs that should be used for compaction. For Jws2020 it is highly likely that
     * @return a JSON-LD structure in compacted format that contains the signed content (e.g. a VC).
     */
    public static JsonObject compact(Issuer issuer, String... additionalContexts) {
        try {
            var expanded = issuer.getExpanded();
            var arrayBuilder = Json.createArrayBuilder();
            Arrays.stream(additionalContexts).forEach(arrayBuilder::add);
            var context = arrayBuilder
                    .add("https://www.w3.org/2018/credentials/v1")
                    .add("https://w3id.org/security/suites/jws-2020/v1")
                    .add("https://www.w3.org/ns/did/v1")
                    .build();
            return JsonLd.compact(JsonDocument.of(expanded), JsonDocument.of(context)).loader(getLoader(issuer))
                    .get();

        } catch (JsonLdError | SigningError | DocumentError e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * rather crude hack to obtain the {@link Issuer}'s loader. The EDC util we're using here basically fetches the declared field recursively.
     *
     * @see ReflectionUtil#getFieldValue(String, Object)
     */
    private static DocumentLoader getLoader(Issuer issuer) {
        return ReflectionUtil.getFieldValue("loader", issuer);
    }
}
