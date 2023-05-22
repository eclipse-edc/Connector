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
import org.eclipse.edc.runtime.metamodel.annotation.BaseExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.CoreConstants;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.String.format;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_SCHEMA;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.spi.CoreConstants.JSON_LD;

/**
 * Adds support for working with JSON-LD. Provides an ObjectMapper that works with Jakarta JSON-P
 * types through the TypeManager context {@link CoreConstants#JSON_LD} and a registry
 * for {@link JsonLdTransformer}s. The module also offers
 * functions for working with JSON-LD structures.
 */
@BaseExtension
@Extension(value = JsonLdExtension.NAME)
public class JsonLdExtension implements ServiceExtension {

    public static final String NAME = "JSON-LD Extension";

    private static final boolean DEFAULT_HTTP_HTTPS_RESOLUTION = false;
    @Setting(value = "If set enable http json-ld document resolution", type = "boolean", defaultValue = DEFAULT_HTTP_HTTPS_RESOLUTION + "")
    private static final String HTTP_ENABLE_SETTING = "edc.jsonld.http.enabled";
    @Setting(value = "If set enable https json-ld document resolution", type = "boolean", defaultValue = DEFAULT_HTTP_HTTPS_RESOLUTION + "")
    private static final String HTTPS_ENABLE_SETTING = "edc.jsonld.https.enabled";

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
        var config = context.getConfig();
        var configuration = JsonLdConfiguration.Builder.newInstance()
                .httpEnabled(config.getBoolean(HTTP_ENABLE_SETTING, DEFAULT_HTTP_HTTPS_RESOLUTION))
                .httpsEnabled(config.getBoolean(HTTPS_ENABLE_SETTING, DEFAULT_HTTP_HTTPS_RESOLUTION))
                .build();
        var monitor = context.getMonitor();
        var service = new TitaniumJsonLd(monitor, configuration);
        service.registerNamespace(EDC_PREFIX, EDC_NAMESPACE);
        service.registerNamespace(DCAT_PREFIX, DCAT_SCHEMA);
        service.registerNamespace(DCT_PREFIX, DCT_SCHEMA);
        service.registerNamespace(ODRL_PREFIX, ODRL_SCHEMA);
        service.registerNamespace(DSPACE_PREFIX, DSPACE_SCHEMA);

        getResourceFile("document" + File.separator + "odrl.jsonld")
                .onSuccess(file -> service.registerCachedDocument("http://www.w3.org/ns/odrl.jsonld", file))
                .onFailure(failure -> monitor.warning("Failed to register cached json-ld document: " + failure.getFailureDetail()));

        return service;
    }

    @NotNull
    private Result<File> getResourceFile(String name) {
        try (var stream = getClass().getClassLoader().getResourceAsStream(name)) {
            if (stream == null) {
                return Result.failure(format("Cannot find resource %s", name));
            }
            var filename = Path.of(name).getFileName().toString();
            var parts = filename.split("\\.");
            var tempFile = Files.createTempFile(parts[0], "." + parts[1]);
            Files.copy(stream, tempFile, REPLACE_EXISTING);
            return Result.success(tempFile.toFile());
        } catch (Exception e) {
            return Result.failure(format("Cannot read resource %s: ", name));
        }
    }

}
