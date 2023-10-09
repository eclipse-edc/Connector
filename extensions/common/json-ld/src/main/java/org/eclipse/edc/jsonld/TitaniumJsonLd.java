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

package org.eclipse.edc.jsonld;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import com.apicatalog.jsonld.loader.FileLoader;
import com.apicatalog.jsonld.loader.HttpLoader;
import com.apicatalog.jsonld.loader.SchemeRouter;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.document.JarLoader;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createBuilderFactory;
import static jakarta.json.Json.createObjectBuilder;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

/**
 * Implementation of the {@link JsonLd} interface that uses the Titanium library for all JSON-LD operations.
 */
public class TitaniumJsonLd implements JsonLd {
    private static final Map<String, String> EMPTY_NAMESPACES = Collections.emptyMap();

    private static final Set<String> EMPTY_CONTEXTS = Collections.emptySet();

    private final Monitor monitor;
    private final Map<String, Map<String, String>> scopedNamespaces = new HashMap<>();
    private final Map<String, Set<String>> scopedContexts = new HashMap<>();
    private final CachedDocumentLoader documentLoader;

    public TitaniumJsonLd(Monitor monitor) {
        this(monitor, JsonLdConfiguration.Builder.newInstance().build());
    }

    public TitaniumJsonLd(Monitor monitor, JsonLdConfiguration configuration) {
        this.monitor = monitor;
        this.documentLoader = new CachedDocumentLoader(configuration);
    }

    @Override
    public Result<JsonObject> expand(JsonObject json) {
        try {
            var document = JsonDocument.of(injectVocab(json));
            var expanded = com.apicatalog.jsonld.JsonLd.expand(document)
                    .options(new JsonLdOptions(documentLoader))
                    .get();
            if (expanded.size() > 0) {
                return Result.success(expanded.getJsonObject(0));
            }
            return Result.failure("Error expanding JSON-LD structure: result was empty, it could be caused by missing '@context'");
        } catch (JsonLdError error) {
            monitor.warning("Error expanding JSON-LD structure", error);
            return Result.failure(error.getMessage());
        }
    }

    @Override
    public Result<JsonObject> compact(JsonObject json, String scope) {
        try {
            var document = JsonDocument.of(json);
            var jsonFactory = createBuilderFactory(Map.of());
            var contextDocument = JsonDocument.of(jsonFactory.createObjectBuilder()
                    .add(CONTEXT, createContext(scope))
                    .build());
            var compacted = com.apicatalog.jsonld.JsonLd.compact(document, contextDocument)
                    .options(new JsonLdOptions(documentLoader))
                    .get();
            return Result.success(compacted);
        } catch (JsonLdError e) {
            monitor.warning("Error compacting JSON-LD structure", e);
            return Result.failure(e.getMessage());
        }
    }

    @Override
    public void registerNamespace(String prefix, String contextIri, String scope) {
        var namespaces = scopedNamespaces.computeIfAbsent(scope, k -> new LinkedHashMap<>());
        namespaces.put(prefix, contextIri);
    }

    @Override
    public void registerContext(String contextIri, String scope) {
        var contexts = scopedContexts.computeIfAbsent(scope, k -> new LinkedHashSet<>());
        contexts.add(contextIri);
    }

    @Override
    public void registerCachedDocument(String contextUrl, URI uri) {
        documentLoader.register(contextUrl, uri);
    }

    private JsonObject injectVocab(JsonObject json) {
        var jsonObjectBuilder = createObjectBuilder(json);

        //only inject the vocab if the @context is an object, not a URL
        if (json.get(CONTEXT) instanceof JsonObject) {
            var contextObject = ofNullable(json.getJsonObject(CONTEXT)).orElseGet(() -> createObjectBuilder().build());
            var contextBuilder = createObjectBuilder(contextObject);
            if (!contextObject.containsKey(VOCAB)) {
                var newContextObject = contextBuilder
                        .add(VOCAB, EDC_NAMESPACE)
                        .build();
                jsonObjectBuilder.add(CONTEXT, newContextObject);
            }
        }
        return jsonObjectBuilder.build();
    }

    private JsonValue createContext(String scope) {
        var builder = createObjectBuilder();
        // Adds the configured namespaces for * and the input scope
        Stream.concat(namespacesForScope(DEFAULT_SCOPE), namespacesForScope(scope))
                .forEach(entry -> builder.add(entry.getKey(), entry.getValue()));

        // Compute the additional context IRI defined for * and the input scope
        var contexts = Stream.concat(contextsForScope(DEFAULT_SCOPE), contextsForScope(scope))
                .collect(Collectors.toSet());

        var contextObject = builder.build();
        // if not empty we build a JsonArray
        if (!contexts.isEmpty()) {
            var contextArray = createArrayBuilder();
            contexts.forEach(contextArray::add);

            // don't append an empty object
            if (!contextObject.isEmpty()) {
                contextArray.add(contextObject);
            }
            return contextArray.build();
        } else {
            // return only the JsonObject with the namespaces
            return contextObject;
        }
    }

    private Stream<Map.Entry<String, String>> namespacesForScope(String scope) {
        return scopedNamespaces.getOrDefault(scope, EMPTY_NAMESPACES).entrySet().stream();
    }

    private Stream<String> contextsForScope(String scope) {
        return scopedContexts.getOrDefault(scope, EMPTY_CONTEXTS).stream();
    }

    private static class CachedDocumentLoader implements DocumentLoader {

        private final Map<String, URI> cache = new HashMap<>();
        private final DocumentLoader loader;

        CachedDocumentLoader(JsonLdConfiguration configuration) {
            loader = new SchemeRouter()
                    .set("http", configuration.isHttpEnabled() ? HttpLoader.defaultInstance() : null)
                    .set("https", configuration.isHttpsEnabled() ? HttpLoader.defaultInstance() : null)
                    .set("file", new FileLoader())
                    .set("jar", new JarLoader());
        }

        @Override
        public Document loadDocument(URI url, DocumentLoaderOptions options) throws JsonLdError {
            var uri = Optional.of(url.toString())
                    .map(cache::get)
                    .orElse(url);

            return loader.loadDocument(uri, options);
        }

        public void register(String contextUrl, URI uri) {
            cache.put(contextUrl, uri);
        }

    }

}
