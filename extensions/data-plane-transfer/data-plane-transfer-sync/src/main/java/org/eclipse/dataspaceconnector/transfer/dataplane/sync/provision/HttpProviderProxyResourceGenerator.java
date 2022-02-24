/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.transfer.dataplane.sync.provision;

import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Generates resource definition for a http proxy serving data on provider side.
 */
public class HttpProviderProxyResourceGenerator implements ResourceDefinitionGenerator {

    @Override
    public @Nullable ResourceDefinition generate(TransferProcess process) {
        return HttpProviderProxyResourceDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .transferProcessId(process.getId())
                .type(process.getDataRequest().getDestinationType())
                .assetId(process.getDataRequest().getAssetId())
                .contractId(process.getDataRequest().getContractId())
                .build();
    }
}
