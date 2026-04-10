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

package org.eclipse.edc.jsonld;

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdTransformer;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.constants.CoreConstants;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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

    @SettingContext(EDC_JSONLD_DOCUMENT_PREFIX)
    @Configuration
    private Map<String, JsonLdDocumentConfiguration> jsonLdConfigurations = new HashMap<>();

    private static final boolean DEFAULT_HTTP_HTTPS_RESOLUTION = false;
    private static final boolean DEFAULT_AVOID_VOCAB_CONTEXT = false;
    private static final boolean DEFAULT_CHECK_PREFIXES = true;
    @Setting(description = "If set enable http json-ld document resolution", defaultValue = DEFAULT_HTTP_HTTPS_RESOLUTION + "", key = "edc.jsonld.http.enabled")
    private boolean httpResolutionEnabled;
    @Setting(description = "If set enable https json-ld document resolution", defaultValue = DEFAULT_HTTP_HTTPS_RESOLUTION + "", key = "edc.jsonld.https.enabled")
    private boolean httpsResolutionEnabled;
    @Setting(description = "If true disable the @vocab context definition. This could be used to avoid api breaking changes", defaultValue = DEFAULT_AVOID_VOCAB_CONTEXT + "", key = "edc.jsonld.vocab.disable")
    private boolean avoidVocab;
    @Setting(description = "If true a validation on expended object will be made against configured prefixes", defaultValue = DEFAULT_CHECK_PREFIXES + "", key = "edc.jsonld.prefixes.check")
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

        CachedDocumentRegistry.getDocuments().forEach(result -> result
                .onSuccess(c -> service.registerCachedDocument(c.url(), c.resource()))
                .onFailure(failure -> monitor.warning("Failed to register cached json-ld document: " + failure.getFailureDetail()))
        );

        jsonLdConfigurations
                .forEach((alias, config) -> {
                    service.registerCachedDocument(config.url(), new File(config.path()).toURI());
                });

        return service;
    }

    @Settings
    private record JsonLdDocumentConfiguration(
            @Setting(key = "path", description = "Path of the JSON-LD document to cache")
            String path,
            @Setting(key = "url", description = "URL of the JSON-LD document to cache")
            String url
    ) {

    }

}
