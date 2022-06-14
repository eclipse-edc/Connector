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


import com.siemens.mindsphere.provision.SourceUrlProvisionedResource;
import com.siemens.mindsphere.provision.SourceUrlProvisioner;
import com.siemens.mindsphere.provision.SourceUrlResourceDefinition;
import com.siemens.mindsphere.provision.SourceUrlResourceDefinitionGenerator;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

public class SourceUrlExtension implements ServiceExtension {

    @Inject
    private ResourceManifestGenerator manifestGenerator;

    @Inject
    private PipelineService pipelineService;

    @Inject
    private DataTransferExecutorServiceContainer executorContainer;


    @Override
    public void initialize(ServiceExtensionContext context) {
        Monitor monitor = context.getMonitor();

        // register provisioner
        @SuppressWarnings("unchecked") var retryPolicy = (RetryPolicy<Object>) context.getService(RetryPolicy.class);
        var provisionManager = context.getService(ProvisionManager.class);
        final SourceUrlProvisioner sourceUrlProvisioner = new SourceUrlProvisioner(context, retryPolicy);
        provisionManager.register(sourceUrlProvisioner);

        // register the datalake resource definition generator
        manifestGenerator.registerGenerator(new SourceUrlResourceDefinitionGenerator(monitor, context));

        // register provision specific classes
        registerTypes(context.getTypeManager());
    }

    @Override
    public String name() {
        return "Datalake to Filesystem Transfer With Provisioning";
    }

    private void registerTypes(TypeManager typeManager) {
        typeManager.registerTypes(SourceUrlProvisionedResource.class, SourceUrlResourceDefinition.class);
    }

}
