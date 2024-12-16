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

package org.eclipse.edc.connector.api.management.version;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.eclipse.edc.connector.api.management.version.v1.VersionApiController;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.apiversion.ApiVersionService;
import org.eclipse.edc.spi.system.apiversion.VersionRecord;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappings;

import java.io.IOException;
import java.util.stream.Stream;

@Extension(value = VersionApiExtension.NAME)
public class VersionApiExtension implements ServiceExtension {

    public static final String NAME = "Management API: Version Information";

    private static final String DEFAULT_VERSION_PATH = "/.well-known/api";
    private static final int DEFAULT_VERSION_PORT = 7171;

    private static final String API_VERSION_JSON_FILE = "version-api-version.json";
    @Configuration
    private VersionApiConfiguration apiConfiguration;
    @Inject
    private WebService webService;
    @Inject
    private TypeManager typeManager;
    @Inject
    private ApiVersionService apiVersionService;
    @Inject
    private PortMappings portMappings;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var portMapping = new PortMapping(ApiContext.VERSION, apiConfiguration.port(), apiConfiguration.path());
        portMappings.register(portMapping);

        webService.registerResource(ApiContext.VERSION, new VersionApiController(apiVersionService));
        registerVersionInfo(getClass().getClassLoader());
    }

    private void registerVersionInfo(ClassLoader resourceClassLoader) {
        try (var versionContent = resourceClassLoader.getResourceAsStream(API_VERSION_JSON_FILE)) {
            if (versionContent == null) {
                throw new EdcException("Version file not found or not readable.");
            }
            Stream.of(typeManager.getMapper()
                            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                            .readValue(versionContent, VersionRecord[].class))
                    .forEach(vr -> apiVersionService.addRecord(ApiContext.VERSION, vr));
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Settings
    record VersionApiConfiguration(
            @Setting(key = "web.http." + ApiContext.VERSION + ".port", description = "Port for " + ApiContext.VERSION + " api context", defaultValue = DEFAULT_VERSION_PORT + "")
            int port,
            @Setting(key = "web.http." + ApiContext.VERSION + ".path", description = "Path for " + ApiContext.VERSION + " api context", defaultValue = DEFAULT_VERSION_PATH)
            String path
    ) {

    }
}
