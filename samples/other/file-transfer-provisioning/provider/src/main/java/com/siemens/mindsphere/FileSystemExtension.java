/*
 *  Copyright (c) 2021, 2022 Siemens AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *
 */

package com.siemens.mindsphere;

import com.siemens.mindsphere.provision.*;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

public class FileSystemExtension implements ServiceExtension {

    @Inject
    private ResourceManifestGenerator manifestGenerator;

    @Override
    public void initialize(ServiceExtensionContext context) {
        Monitor monitor = context.getMonitor();

        // register provisioner
        @SuppressWarnings("unchecked") var retryPolicy = (RetryPolicy<Object>) context.getService(RetryPolicy.class);
        var provisionManager = context.getService(ProvisionManager.class);
        final FileSystemProvisioner fileSystemProvisioner = new FileSystemProvisioner(monitor, retryPolicy);
        final SourceUrlProvisioner sourceUrlProvisioner = new SourceUrlProvisioner(context, retryPolicy);

        provisionManager.register(fileSystemProvisioner);
        provisionManager.register(sourceUrlProvisioner);

        // register the fs resource definition generator
        manifestGenerator.registerGenerator(new FileSystemResourceDefinitionGenerator(monitor, context));

        // register provision specific classes
        registerTypes(context.getTypeManager());
    }

    @Override
    public String name() {
        return "FileSystem Transfer With Provisioning";
    }

    private void registerTypes(TypeManager typeManager) {
        typeManager.registerTypes(FileSystemProvisionedResource.class, FileSystemResourceDefinition.class);
        typeManager.registerTypes(SourceUrlProvisionedResource.class, SourceUrlResourceDefinition.class);
    }
}
