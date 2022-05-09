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

package com.siemens.mindsphere.provision;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProviderResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.jetbrains.annotations.Nullable;

import static java.util.UUID.randomUUID;

public class FileSystemResourceDefinitionGenerator implements ProviderResourceDefinitionGenerator {
    public FileSystemResourceDefinitionGenerator(Monitor monitor, ServiceExtensionContext context) {
        this.monitor = monitor;
        this.context = context;
    }

    private static final String EDC_ASSET_PATH = "edc.samples.04.asset.path";

    private Monitor monitor;
    private ServiceExtensionContext context;

    @Override
    public @Nullable ResourceDefinition generate(DataRequest dataRequest, DataAddress assetAddress, Policy policy) {
        if (dataRequest.getDestinationType() == null) {
            return null;
        }

        final String dataDestinationType = dataRequest.getDataDestination().getType();
        if (!"file".equals(dataDestinationType)) {
            return null;
        }

        var assetPathSetting = context.getSetting(EDC_ASSET_PATH, "/tmp/provider/test-document.txt");

        monitor.info("Generating source path for dataRequest: " + dataRequest.getId());

        return FileSystemResourceDefinition.Builder.newInstance()
                .id(randomUUID().toString())
                .path(assetPathSetting)
                .build();
    }
}
