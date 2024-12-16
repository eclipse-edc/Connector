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

package org.eclipse.edc.api.iam.identitytrust.sts.accounts;

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

@Extension(value = StsAccountsApiConfigurationExtension.NAME)
public class StsAccountsApiConfigurationExtension implements ServiceExtension {

    public static final String NAME = "Secure Token Service Accounts API configuration";
    private static final int DEFAULT_STS_ACCOUNTS_PORT = 9393;
    private static final String DEFAULT_STS_ACCOUNTS_PATH = "/api/accounts";
    private static final String API_VERSION_JSON_FILE = "sts-accounts-api-version.json";

    @Configuration
    private StsAccountApiConfiguration apiConfiguration;
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
        var portMapping = new PortMapping(ApiContext.STS_ACCOUNTS, apiConfiguration.port(), apiConfiguration.path());
        portMappings.register(portMapping);
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
                    .forEach(vr -> apiVersionService.addRecord(ApiContext.STS_ACCOUNTS, vr));
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Settings
    record StsAccountApiConfiguration(
            @Setting(key = "web.http." + ApiContext.STS_ACCOUNTS + ".port", description = "Port for " + ApiContext.STS_ACCOUNTS + " api context", defaultValue = DEFAULT_STS_ACCOUNTS_PORT + "")
            int port,
            @Setting(key = "web.http." + ApiContext.STS_ACCOUNTS + ".path", description = "Path for " + ApiContext.STS_ACCOUNTS + " api context", defaultValue = DEFAULT_STS_ACCOUNTS_PATH)
            String path
    ) {

    }
}
