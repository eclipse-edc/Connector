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

package org.eclipse.edc.connector.controlplane.provision.http;

import org.eclipse.edc.connector.controlplane.provision.http.config.ConfigParser;
import org.eclipse.edc.connector.controlplane.provision.http.config.ProvisionerConfiguration;
import org.eclipse.edc.connector.controlplane.provision.http.impl.HttpProviderProvisioner;
import org.eclipse.edc.connector.controlplane.provision.http.impl.HttpProviderResourceDefinition;
import org.eclipse.edc.connector.controlplane.provision.http.impl.HttpProviderResourceDefinitionGenerator;
import org.eclipse.edc.connector.controlplane.provision.http.impl.HttpProvisionedContentResource;
import org.eclipse.edc.connector.controlplane.provision.http.impl.HttpProvisionerRequest;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.validator.dataaddress.httpdata.HttpDataDataAddressValidator;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;

import static java.lang.String.format;

/**
 * The HTTP Provisioner extension delegates to HTTP endpoints to perform provision operations.
 *
 * @deprecated provisioning has been moved to data-plane
 */
@Extension(value = HttpProvisionerExtension.NAME)
@Deprecated(since = "0.14.0")
public class HttpProvisionerExtension implements ServiceExtension {

    public static final String NAME = "DEPRECATED: HTTP Provisioning";

    public static final String DEFAULT_POLICY_SCOPE = "http.provisioner";
    public static final String HTTP_PROVISIONER_ENTRIES = "provisioner.http.entries";

    @Setting(required = true)
    public static final String PROVISIONER_TYPE = "provisioner.type";

    @Setting(required = true)
    public static final String DATA_ADDRESS_TYPE = "data.address.type";

    @Setting(required = true)
    public static final String ENDPOINT_URL = "endpoint";

    @Setting(defaultValue = DEFAULT_POLICY_SCOPE)
    public static final String POLICY_SCOPE = "policy.scope";

    @Inject
    protected ProvisionManager provisionManager;
    @Inject
    protected PolicyEngine policyEngine;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private ResourceManifestGenerator manifestGenerator;

    @Inject
    private HttpProvisionerWebhookUrl callbackUrl;

    @Inject
    private TypeManager typeManager;

    @Inject
    private DataAddressValidatorRegistry dataAddressValidatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        var configurations = ConfigParser.parseConfigurations(context.getConfig());

        var monitor = context.getMonitor();

        for (var configuration : configurations) {
            var provisioner = new HttpProviderProvisioner(configuration, callbackUrl.get(), policyEngine, httpClient, typeManager.getMapper(), monitor);

            if (configuration.getProvisionerType() == ProvisionerConfiguration.ProvisionerType.PROVIDER) {
                var generator = new HttpProviderResourceDefinitionGenerator(configuration.getDataAddressType());
                manifestGenerator.registerGenerator(generator);
                monitor.debug(format("Registering provider provisioner: %s [%s]", configuration.getName(), configuration.getEndpoint().toString()));
            } else {
                monitor.warning(format("Client-side provisioning not yet supported by the %s. Skipping configuration for %s", name(), configuration.getName()));
            }

            provisionManager.register(provisioner);
        }

        dataAddressValidatorRegistry.registerSourceValidator("HttpProvision", new HttpDataDataAddressValidator());

        typeManager.registerTypes(
                HttpProviderResourceDefinition.class,
                HttpProvisionedContentResource.class,
                HttpProvisionerRequest.class);
    }


}
