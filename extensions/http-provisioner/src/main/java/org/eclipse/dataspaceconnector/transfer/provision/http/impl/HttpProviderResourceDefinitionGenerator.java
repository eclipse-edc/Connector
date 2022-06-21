/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transfer.provision.http.impl;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProviderResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Generates {@link HttpProviderResourceDefinition}s for data addresses matching a type.
 */
public class HttpProviderResourceDefinitionGenerator implements ProviderResourceDefinitionGenerator {
    private final String dataAddressType;

    public HttpProviderResourceDefinitionGenerator(String dataAddressType) {
        this.dataAddressType = requireNonNull(dataAddressType);
    }

    @Override
    public @Nullable ResourceDefinition generate(DataRequest dataRequest, DataAddress assetAddress, Policy policy) {
        if (!dataAddressType.equals(assetAddress.getType())) {
            return null;
        }
        var assetId = dataRequest.getAssetId();
        if (assetId == null) {
            // programming error
            throw new EdcException("Asset id was null for request: " + dataRequest.getId());
        }
        return HttpProviderResourceDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .dataAddressType(dataAddressType)
                .transferProcessId(dataRequest.getProcessId())
                .assetId(assetId)
                .build();
    }
}
