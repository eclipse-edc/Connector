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

package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides({ AssetIndex.class, DataAddressResolver.class, AssetLoader.class })
public class InMemoryAssetIndexExtension implements ServiceExtension {

    @Override
    public String name() {
        return "In-Memory Asset Index";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var service = new InMemoryAssetIndex();
        context.registerService(AssetIndex.class, service);
        context.registerService(AssetLoader.class, service);
        context.registerService(DataAddressResolver.class, service);
    }
}
