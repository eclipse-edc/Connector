/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.provision.http;

import org.eclipse.edc.connector.dataplane.provision.http.logic.ProvisionHttpResourceDefinitionGenerator;
import org.eclipse.edc.connector.dataplane.provision.http.logic.ProvisionHttpResourceDeprovisioner;
import org.eclipse.edc.connector.dataplane.provision.http.logic.ProvisionHttpResourceProvisioner;
import org.eclipse.edc.connector.dataplane.provision.http.port.ProvisionHttpApiController;
import org.eclipse.edc.connector.dataplane.provision.http.port.ProvisionHttpClient;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionerManager;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

@Extension(DataPlaneProvisionHttpExtension.NAME)
public class DataPlaneProvisionHttpExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Provision Http";
    private static final int DEFAULT_PROVISION_PORT = 8765;
    private static final String DEFAULT_PROVISION_PATH = "/provisioning";

    @Configuration
    private ProvisionApiConfiguration apiConfiguration;
    @Setting(description = "Configures endpoint for reaching the Provision API", key = "edc.dataplane.provision.http.endpoint", required = false)
    private String provisionApiEndpoint;

    @Inject
    private ResourceDefinitionGeneratorManager resourceDefinitionGeneratorManager;
    @Inject
    private ProvisionerManager provisionerManager;
    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private Monitor monitor;
    @Inject
    private TypeManager typeManager;
    @Inject
    private WebService webService;
    @Inject
    private DataPlaneManager dataPlaneManager;
    @Inject
    private Hostname hostname;
    @Inject
    private PortMappingRegistry portMappingRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var portMapping = new PortMapping("provision", apiConfiguration.port(), apiConfiguration.path());
        portMappingRegistry.register(portMapping);
        var callbackAddress = ofNullable(provisionApiEndpoint).orElseGet(() -> format("http://%s:%s%s", hostname.get(), portMapping.port(), portMapping.path()));

        var provisionHttpClient = new ProvisionHttpClient(callbackAddress, httpClient, typeManager.getMapper());

        resourceDefinitionGeneratorManager.registerProviderGenerator(new ProvisionHttpResourceDefinitionGenerator());
        provisionerManager.register(new ProvisionHttpResourceProvisioner(provisionHttpClient));
        provisionerManager.register(new ProvisionHttpResourceDeprovisioner(provisionHttpClient));

        webService.registerResource("provision", new ProvisionHttpApiController(dataPlaneManager));
    }

    @Settings
    record ProvisionApiConfiguration(
            @Setting(key = "web.http.provision.port", description = "Port for provision api context", defaultValue = DEFAULT_PROVISION_PORT + "")
            int port,
            @Setting(key = "web.http.provision.path", description = "Path for provision api context", defaultValue = DEFAULT_PROVISION_PATH)
            String path
    ) {

    }
}
