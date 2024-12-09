/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld;

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdTransformer;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.constants.CoreConstants;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_CONTEXT_2025_1;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_ODRL_PROFILE_2025_1;
import static org.eclipse.edc.jsonld.spi.Namespaces.EDC_DSPACE_CONTEXT;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Adds support for working with JSON-LD. Provides an ObjectMapper that works with Jakarta JSON-P
 * types through the TypeManager context {@link CoreConstants#JSON_LD} and a registry
 * for {@link JsonLdTransformer}s. The module also offers
 * functions for working with JSON-LD structures.
 */
@Extension(value = JsonLdExtension.NAME)
public class JsonLdExtension implements ServiceExtension {

    public static final String NAME = "JSON-LD Extension";
    public static final String EDC_JSONLD_DOCUMENT_PREFIX = "edc.jsonld.document";
    public static final String EDC_JSONLD_DOCUMENT_CONFIG_ALIAS = EDC_JSONLD_DOCUMENT_PREFIX + ".<documentAlias>.";

    @Setting(context = EDC_JSONLD_DOCUMENT_CONFIG_ALIAS, value = "Path of the JSON-LD document to cache", required = true)
    public static final String CONFIG_VALUE_PATH = "path";
    @Setting(context = EDC_JSONLD_DOCUMENT_CONFIG_ALIAS, value = "URL of the JSON-LD document to cache", required = true)
    public static final String CONFIG_VALUE_URL = "url";

    private static final boolean DEFAULT_HTTP_HTTPS_RESOLUTION = false;
    private static final boolean DEFAULT_AVOID_VOCAB_CONTEXT = false;
    private static final boolean DEFAULT_CHECK_PREFIXES = true;
    @Setting(description = "If set enable http json-ld document resolution", defaultValue = DEFAULT_HTTP_HTTPS_RESOLUTION + "", key = "edc.jsonld.http.enabled")
    private boolean httpResolutionEnabled;
    @Setting(description = "If set enable https json-ld document resolution", type = "boolean", defaultValue = DEFAULT_HTTP_HTTPS_RESOLUTION + "", key = "edc.jsonld.https.enabled")
    private boolean httpsResolutionEnabled;
    @Setting(description = "If true disable the @vocab context definition. This could be used to avoid api breaking changes", defaultValue = DEFAULT_AVOID_VOCAB_CONTEXT + "", key = "edc.jsonld.vocab.disable")
    private boolean avoidVocab;
    @Setting(description = "If true a validation on expended object will be made against configured prefixes", type = "boolean", defaultValue = DEFAULT_CHECK_PREFIXES + "", key = "edc.jsonld.prefixes.check")
    private boolean checkPrefixes;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        typeManager.registerContext(JSON_LD, JacksonJsonLd.createObjectMapper());
    }

    @Provider
    public JsonLd createJsonLdService(ServiceExtensionContext context) {
        var configuration = JsonLdConfiguration.Builder.newInstance()
                .httpEnabled(httpResolutionEnabled)
                .httpsEnabled(httpsResolutionEnabled)
                .avoidVocab(avoidVocab)
                .checkPrefixes(checkPrefixes)
                .build();
        var monitor = context.getMonitor();
        var service = new TitaniumJsonLd(monitor, configuration);

        Stream.of(
                new JsonLdContext("odrl.jsonld", "http://www.w3.org/ns/odrl.jsonld"),
                new JsonLdContext("dspace.jsonld", "https://w3id.org/dspace/2024/1/context.json"),
                new JsonLdContext("management-context-v1.jsonld", EDC_CONNECTOR_MANAGEMENT_CONTEXT),
                new JsonLdContext("dspace-edc-context-v1.jsonld", EDC_DSPACE_CONTEXT),
                new JsonLdContext("dspace-v2025-1.jsonld", DSPACE_CONTEXT_2025_1),
                new JsonLdContext("dspace-v2025-1-odrl.jsonld", DSPACE_ODRL_PROFILE_2025_1)
        ).forEach(jsonLdContext -> getResourceUri("document/" + jsonLdContext.fileName())
                .onSuccess(uri -> service.registerCachedDocument(jsonLdContext.url(), uri))
                .onFailure(failure -> monitor.warning("Failed to register cached json-ld document: " + failure.getFailureDetail()))
        );

        registerCachedDocumentsFromConfig(context, service);

        return service;
    }

    private void registerCachedDocumentsFromConfig(ServiceExtensionContext context, TitaniumJsonLd service) {
        context.getConfig()
                .getConfig(EDC_JSONLD_DOCUMENT_PREFIX)
                .partition()
                .forEach(config -> {
                    var tuple = config.getRelativeEntries();
                    if (tuple.containsKey(CONFIG_VALUE_PATH) && tuple.containsKey(CONFIG_VALUE_URL)) {
                        service.registerCachedDocument(tuple.get(CONFIG_VALUE_URL), new File(tuple.get(CONFIG_VALUE_PATH)).toURI());
                    } else {
                        context.getMonitor().warning(format("Expected a '%s' and a '%s' entry for '%s.%s', but found only '%s'", CONFIG_VALUE_PATH, CONFIG_VALUE_URL, EDC_JSONLD_DOCUMENT_PREFIX, config.currentNode(), String.join("", tuple.keySet())));
                    }
                });
    }

    @NotNull
    private Result<URI> getResourceUri(String name) {
        var uri = getClass().getClassLoader().getResource(name);
        if (uri == null) {
            return Result.failure(format("Cannot find resource %s", name));
        }

        try {
            return Result.success(uri.toURI());
        } catch (URISyntaxException e) {
            return Result.failure(format("Cannot read resource %s: %s", name, e.getMessage()));
        }
    }

    record JsonLdContext(String fileName, String url) {
    }

}
