/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.provision.http;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.transfer.provision.http.impl.HttpProviderProvisioner;
import org.eclipse.dataspaceconnector.transfer.provision.http.impl.HttpProviderResourceDefinition;
import org.eclipse.dataspaceconnector.transfer.provision.http.impl.HttpProviderResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.transfer.provision.http.impl.HttpProvisionedContentResource;
import org.eclipse.dataspaceconnector.transfer.provision.http.impl.HttpProvisionerRequest;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.transfer.provision.http.config.ConfigParser.parseConfigurations;
import static org.eclipse.dataspaceconnector.transfer.provision.http.config.ProvisionerConfiguration.ProvisionerType.PROVIDER;

/**
 * The HTTP Provisioner extension delegates to HTTP endpoints to perform provision operations.
 */
public class HttpProvisionerExtension implements ServiceExtension {

    @Inject
    protected ProvisionManager provisionManager;
    @Inject
    protected PolicyEngine policyEngine;
    @Inject
    protected OkHttpClient httpClient;
    @Inject
    private ResourceManifestGenerator manifestGenerator;
    private OkHttpClient overrideHttpClient;

    @Inject
    private HttpProvisionerWebhookUrl callbackUrl;

    /**
     * Default ctor.
     */
    @SuppressWarnings("unused")
    public HttpProvisionerExtension() {
    }

    /**
     * Overrides the default HTTP client. Intended for testing.
     */
    public HttpProvisionerExtension(OkHttpClient httpClient) {
        overrideHttpClient = httpClient;
    }

    @Override
    public String name() {
        return "HTTP Provisioning";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        var configurations = parseConfigurations(context.getConfig());

        var client = overrideHttpClient != null ? overrideHttpClient : httpClient;
        var typeManager = context.getTypeManager();
        var monitor = context.getMonitor();

        for (var configuration : configurations) {

            var provisioner = new HttpProviderProvisioner(configuration, callbackUrl.get(), policyEngine, client, typeManager.getMapper(), monitor);

            if (configuration.getProvisionerType() == PROVIDER) {
                var generator = new HttpProviderResourceDefinitionGenerator(configuration.getDataAddressType());
                manifestGenerator.registerGenerator(generator);
                monitor.info(format("Registering provider provisioner: %s [%s]", configuration.getName(), configuration.getEndpoint().toString()));
            } else {
                monitor.warning(format("Client-side provisioning not yet supported by the %s. Skipping configuration for %s", name(), configuration.getName()));
            }

            provisionManager.register(provisioner);
        }

        typeManager.registerTypes(
                HttpProviderResourceDefinition.class,
                HttpProvisionedContentResource.class,
                HttpProvisionerRequest.class);
    }


}
