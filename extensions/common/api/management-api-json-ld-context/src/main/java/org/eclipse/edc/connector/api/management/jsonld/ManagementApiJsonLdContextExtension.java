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

package org.eclipse.edc.connector.api.management.jsonld;

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static java.lang.String.format;

public class ManagementApiJsonLdContextExtension implements ServiceExtension {

    public static final String EDC_CONNECTOR_MANAGEMENT_CONTEXT = "https://w3id.org/edc/connector/management/v0.0.1";
    public static final String EDC_CONNECTOR_MANAGEMENT_SCOPE = "MANAGEMENT_API";

    private static final String PREFIX = "document/";
    private static final Map<String, String> FILES = Map.of(
            EDC_CONNECTOR_MANAGEMENT_CONTEXT, PREFIX + "management-context-v1.jsonld");

    @Inject
    private JsonLd jsonLdService;

    @Inject
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        jsonLdService.registerContext(EDC_CONNECTOR_MANAGEMENT_CONTEXT, EDC_CONNECTOR_MANAGEMENT_SCOPE);
        FILES.entrySet().stream().map(this::mapToUri)
                .forEach(result -> result.onSuccess(entry -> jsonLdService.registerCachedDocument(entry.getKey(), entry.getValue()))
                        .onFailure(failure -> monitor.warning("Failed to register cached json-ld document: " + failure.getFailureDetail())));
    }

    private Result<Map.Entry<String, URI>> mapToUri(Map.Entry<String, String> fileEntry) {
        return getResourceUri(fileEntry.getValue())
                .map(file1 -> Map.entry(fileEntry.getKey(), file1));
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
}
