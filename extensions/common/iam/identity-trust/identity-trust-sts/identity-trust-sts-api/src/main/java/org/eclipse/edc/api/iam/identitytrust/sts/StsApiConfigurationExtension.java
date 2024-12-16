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

package org.eclipse.edc.api.iam.identitytrust.sts;

import com.fasterxml.jackson.databind.DeserializationFeature;
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
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappings;

import java.io.IOException;
import java.util.stream.Stream;

@Extension(value = StsApiConfigurationExtension.NAME)
public class StsApiConfigurationExtension implements ServiceExtension {

    public static final String NAME = "Secure Token Service API configuration";
    static final int DEFAULT_STS_PORT = 9292;
    static final String DEFAULT_STS_PATH = "/api/sts";

    private static final String API_VERSION_JSON_FILE = "sts-api-version.json";

    @Configuration
    private StsApiConfiguration apiConfiguration;
    @Inject
    private PortMappings portMappings;
    @Inject
    private TypeManager typeManager;
    @Inject
    private ApiVersionService apiVersionService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        portMappings.register(new PortMapping(ApiContext.STS, apiConfiguration.port(), apiConfiguration.path()));
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
                    .forEach(vr -> apiVersionService.addRecord(ApiContext.STS, vr));
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Settings
    record StsApiConfiguration(
            @Setting(key = "web.http." + ApiContext.STS + ".port", description = "Port for " + ApiContext.STS + " api context", defaultValue = DEFAULT_STS_PORT + "")
            int port,
            @Setting(key = "web.http." + ApiContext.STS + ".path", description = "Path for " + ApiContext.STS + " api context", defaultValue = DEFAULT_STS_PATH)
            String path
    ) {

    }
}
