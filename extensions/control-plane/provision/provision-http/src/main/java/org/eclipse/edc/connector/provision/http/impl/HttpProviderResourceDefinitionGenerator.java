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
 *       ZF Friedrichshafen AG - improvements (refactoring of canGenerate method)
 *       SAP SE - refactoring
 *
 */

package org.eclipse.edc.connector.provision.http.impl;

import org.eclipse.edc.connector.transfer.spi.provision.ProviderResourceDefinitionGenerator;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.domain.DataAddress;
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

    @Override
    public boolean canGenerate(DataRequest dataRequest, DataAddress assetAddress, Policy policy) {
        return dataAddressType.equals(assetAddress.getType());
    }
}
