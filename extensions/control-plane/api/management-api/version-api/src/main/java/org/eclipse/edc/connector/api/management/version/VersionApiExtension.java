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

import jakarta.json.Json;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;

import java.util.Map;

@Extension(value = VersionApiExtension.NAME)
public class VersionApiExtension implements ServiceExtension {

    public static final String NAME = "Management API: Version Information";

    @Inject
    private WebService webService;

    @Inject
    private ManagementApiConfiguration managementConfig;

    @Inject
    private TypeManager typeManager;


    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerResource(managementConfig.getContextAlias(), new VersionApiController(getClassLoader(), Json.createBuilderFactory(Map.of()), typeManager.getMapper()));
    }

    public ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
