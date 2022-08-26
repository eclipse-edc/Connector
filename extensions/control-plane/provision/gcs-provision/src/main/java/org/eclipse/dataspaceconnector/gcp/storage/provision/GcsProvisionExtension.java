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

import org.eclipse.dataspaceconnector.gcp.lib.iam.IamClientFactory;
import org.eclipse.dataspaceconnector.gcp.lib.iam.IamService;
import org.eclipse.dataspaceconnector.gcp.lib.iam.IamServiceImpl;
import org.eclipse.dataspaceconnector.gcp.lib.storage.DefaultStorageClientFactory;
import org.eclipse.dataspaceconnector.gcp.lib.storage.StorageClientFactory;
import org.eclipse.dataspaceconnector.gcp.lib.storage.StorageService;
import org.eclipse.dataspaceconnector.gcp.lib.storage.StorageServiceImpl;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.EdcSetting;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;


public class GcsProvisionExtension implements ServiceExtension {

    @EdcSetting(required = true)
    private static final String GCP_PROJECT_ID = "edc.gcp.projectId";

    @Inject
    ProvisionManager provisionManager;

    IamService iamService;
    StorageService storageService;
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        String projectId = context.getConfig().getString(GCP_PROJECT_ID);
        StorageClientFactory storageFactory = new DefaultStorageClientFactory(projectId);
        storageService = new StorageServiceImpl(storageFactory, monitor);
        IamClientFactory iamFactory = new IamClientFactory();
        iamService = new IamServiceImpl(iamFactory, monitor, projectId);

        var provisioner = new GcsProvisioner(monitor, storageService, iamService);
        provisionManager.register(provisioner);
    }

    @Override
    public void start() {
        monitor.info("started GCP storage provisioner");
    }

    @Override
    public void shutdown() {
        monitor.info("shutdown GCP storage provisioner");
    }
}