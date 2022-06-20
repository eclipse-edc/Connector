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

package com.siemens.mindsphere.datalake.edc.http.provision;

import io.opentelemetry.api.internal.StringUtils;
import org.eclipse.dataspaceconnector.dataplane.cloud.http.pipeline.PresignedHttpDataAddressSchema;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProviderResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.jetbrains.annotations.Nullable;

import static java.util.UUID.randomUUID;

public class SourceUrlResourceDefinitionGenerator implements ProviderResourceDefinitionGenerator {
    public SourceUrlResourceDefinitionGenerator(Monitor monitor, ServiceExtensionContext context) {
        this.monitor = monitor;
        this.context = context;
    }

    private Monitor monitor;
    private ServiceExtensionContext context;

    @Override
    public @Nullable ResourceDefinition generate(DataRequest dataRequest, DataAddress assetAddress, Policy policy) {
        if (dataRequest.getDestinationType() == null) {
            monitor.debug("There is no destination type");
            return null;
        }

        final String dataDestinationType = dataRequest.getDataDestination().getType();
        if (!PresignedHttpDataAddressSchema.TYPE.equalsIgnoreCase(dataDestinationType)) {
            monitor.debug("The destination is " + dataDestinationType);
            return null;
        }

        final String datalakePath = assetAddress.getProperty(MindsphereDatalakeSchema.DOWNLOAD_DATALAKE_PATH);
        if (StringUtils.isNullOrEmpty(datalakePath)) {
            monitor.debug("There is no " + MindsphereDatalakeSchema.DOWNLOAD_DATALAKE_PATH);
            return null;
        }

        monitor.info("Generating source path for dataRequest: " + dataRequest.getId());

        return SourceUrlResourceDefinition.Builder.newInstance()
                .id(randomUUID().toString())
                .datalakePath(datalakePath)
                .build();
    }
}
