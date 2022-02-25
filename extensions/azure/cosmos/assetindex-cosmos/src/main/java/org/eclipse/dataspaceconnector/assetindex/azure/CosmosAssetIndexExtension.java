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

package org.eclipse.dataspaceconnector.assetindex.azure;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.assetindex.azure.model.AssetDocument;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;

/**
 * Provides a persistent implementation of the {@link org.eclipse.dataspaceconnector.spi.asset.AssetIndex} using CosmosDB.
 */
@Provides({ AssetIndex.class, DataAddressResolver.class, AssetLoader.class })
public class CosmosAssetIndexExtension implements ServiceExtension {

    @Override
    public String name() {
        return "CosmosDB Asset Index";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var configuration = new AssetIndexCosmosConfig(context);
        Vault vault = context.getService(Vault.class);

        var cosmosDbApi = new CosmosDbApiImpl(vault, configuration);
        var assetIndex = new CosmosAssetIndex(cosmosDbApi, configuration.getPartitionKey(), context.getTypeManager(), context.getService(RetryPolicy.class));
        context.registerService(AssetIndex.class, assetIndex);
        context.registerService(AssetLoader.class, assetIndex);
        context.registerService(DataAddressResolver.class, assetIndex);

        context.getService(HealthCheckService.class).addReadinessProvider(() -> cosmosDbApi.get().forComponent(name()));

        context.getTypeManager().registerTypes(AssetDocument.class);
    }

}

