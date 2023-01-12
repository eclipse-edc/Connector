/*
 *  Copyright (c) 2022 Google LLC
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Google LCC - Initial implementation
 *
 */

package org.eclipse.edc.connector.provision.gcp;

import org.eclipse.edc.connector.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.gcp.common.GcpCredentials;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;


public class GcsProvisionExtension implements ServiceExtension {

    @Override
    public String name() {
        return "GCP storage provisioner";
    }

    @Setting(value = "The GCP project ID", required = false)
    private static final String GCP_PROJECT_ID = "edc.gcp.project.id";

    @Inject
    private ProvisionManager provisionManager;

    @Inject
    private ResourceManifestGenerator manifestGenerator;


    @Inject
    private TypeManager typeManager;

    @Inject
    private Vault vault;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        var projectId = context.getConfig().getString(GCP_PROJECT_ID);
        var gcpCredential = new GcpCredentials(vault, typeManager, monitor);

        var provisioner = new GcsProvisioner(monitor, gcpCredential, projectId);
        provisionManager.register(provisioner);

        manifestGenerator.registerGenerator(new GcsConsumerResourceDefinitionGenerator());
    }
}