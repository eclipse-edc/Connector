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

package org.eclipse.dataspaceconnector.gcp.storage.provision;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.eclipse.dataspaceconnector.gcp.core.iam.IamServiceImpl;
import org.eclipse.dataspaceconnector.gcp.core.storage.StorageServiceImpl;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.EdcSetting;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;


public class GcsProvisionExtension implements ServiceExtension {

    @EdcSetting(value = "The GCP project ID", required = true)
    private static final String GCP_PROJECT_ID = "edc.gcp.projectId";

    @Inject
    private ProvisionManager provisionManager;

    @Inject
    private ResourceManifestGenerator manifestGenerator;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var projectId = context.getConfig().getString(GCP_PROJECT_ID);
        var storageClient = createDefaultStorageClient(projectId);
        var storageService = new StorageServiceImpl(storageClient, monitor);
        var iamService = IamServiceImpl.Builder.newInstance(monitor, projectId).build();

        var provisioner = new GcsProvisioner(monitor, storageService, iamService);
        provisionManager.register(provisioner);

        manifestGenerator.registerGenerator(new GcsConsumerResourceDefinitionGenerator());
    }


    /**
     * Creates {@link Storage} for the specified project using application default credentials
     *
     * @param projectId The project that should be used for storage operations
     * @return {@link Storage}
     */
    private Storage createDefaultStorageClient(String projectId) {
        return StorageOptions.newBuilder().setProjectId(projectId).build().getService();
    }

    //    @Override
    //    public void start() {
    //    }
    //
    //    @Override
    //    public void shutdown() {
    //    }
}