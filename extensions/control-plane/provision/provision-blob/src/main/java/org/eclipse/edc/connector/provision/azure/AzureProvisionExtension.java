/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.provision.azure;

import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.azure.blob.AzureSasToken;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.provision.azure.blob.ObjectContainerProvisionedResource;
import org.eclipse.edc.connector.provision.azure.blob.ObjectContainerStatusChecker;
import org.eclipse.edc.connector.provision.azure.blob.ObjectStorageConsumerResourceDefinitionGenerator;
import org.eclipse.edc.connector.provision.azure.blob.ObjectStorageProvisioner;
import org.eclipse.edc.connector.provision.azure.blob.ObjectStorageResourceDefinition;
import org.eclipse.edc.connector.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.transfer.spi.provision.Provisioner;
import org.eclipse.edc.connector.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.connector.transfer.spi.status.StatusCheckerRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

/**
 * Provides data transfer {@link Provisioner}s backed by Azure services.
 */
public class AzureProvisionExtension implements ServiceExtension {

    @Inject
    private BlobStoreApi blobStoreApi;

    @Inject
    private RetryPolicy<Object> retryPolicy;

    @Inject
    private ResourceManifestGenerator manifestGenerator;

    @Inject
    private StatusCheckerRegistry statusCheckerRegistry;

    @Override
    public String name() {
        return "Azure Provision";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        var monitor = context.getMonitor();
        var provisionManager = context.getService(ProvisionManager.class);

        provisionManager.register(new ObjectStorageProvisioner(retryPolicy, monitor, blobStoreApi));

        // register the generator
        manifestGenerator.registerGenerator(new ObjectStorageConsumerResourceDefinitionGenerator());

        statusCheckerRegistry.register(AzureBlobStoreSchema.TYPE, new ObjectContainerStatusChecker(blobStoreApi, retryPolicy));

        registerTypes(context.getTypeManager());
    }

    private void registerTypes(TypeManager typeManager) {
        typeManager.registerTypes(ObjectContainerProvisionedResource.class, ObjectStorageResourceDefinition.class, AzureSasToken.class);
    }

}
