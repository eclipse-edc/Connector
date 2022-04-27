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

package org.eclipse.dataspaceconnector.spi.transfer.provision;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;

/**
 * Generates resource manifests for data transfer requests. Implementations are responsible for enforcing policy constraints associated with transfer requests.
 */
public interface ResourceManifestGenerator {

    /**
     * Registers a generator for consumer-side generation.
     *
     * @param generator the generator
     */
    void registerGenerator(ConsumerResourceDefinitionGenerator generator);

    /**
     * Registers a generator for producer-side generation.
     *
     * @param generator the generator
     */
    void registerGenerator(ProviderResourceDefinitionGenerator generator);

    /**
     * Generates a resource manifest for a consumer-side data request. Operations must be idempotent.
     *
     * @param dataRequest the data request associated with transfer process
     * @param policy      the contract agreement usage policy for the asset being transferred
     */
    ResourceManifest generateConsumerResourceManifest(DataRequest dataRequest, Policy policy);

    /**
     * Generates a resource manifest for a provider-side data request. Operations must be idempotent.
     *
     * @param dataRequest  the data request associated with transfer process
     * @param assetAddress the asset data address
     * @param policy       the contract agreement usage policy for the asset being transferred
     */
    ResourceManifest generateProviderResourceManifest(DataRequest dataRequest, DataAddress assetAddress, Policy policy);
}
