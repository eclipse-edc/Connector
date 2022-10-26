/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.provision.oauth2;

import okhttp3.OkHttpClient;
import org.eclipse.edc.connector.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Extension(value = Oauth2ProvisionExtension.NAME)
public class Oauth2ProvisionExtension implements ServiceExtension {
    static final String NAME = "Oauth2 Provision";

    @Inject
    private ResourceManifestGenerator resourceManifestGenerator;

    @Inject
    private ProvisionManager provisionManager;

    @Inject
    private OkHttpClient httpClient;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var typeManager = context.getTypeManager();
        typeManager.registerTypes(Oauth2ResourceDefinition.class, Oauth2ProvisionedResource.class);

        resourceManifestGenerator.registerGenerator(new Oauth2ProviderResourceDefinitionGenerator());
        resourceManifestGenerator.registerGenerator(new Oauth2ConsumerResourceDefinitionGenerator());

        provisionManager.register(new Oauth2Provisioner(httpClient, typeManager));
    }

}
