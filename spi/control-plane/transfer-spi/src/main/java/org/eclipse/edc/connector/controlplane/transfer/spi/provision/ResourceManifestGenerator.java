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
 *       Fraunhofer Institute for Software and Systems Engineering - add policy scope
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.spi.provision;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceManifest;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * Generates resource manifests for data transfer requests. Implementations are responsible for enforcing policy constraints associated with transfer requests.
 */
@ExtensionPoint
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
     * Generates a resource manifest for a consumer-side transfer process. Operations must be idempotent.
     *
     * @param transferProcess the transfer process
     * @param policy          the contract agreement usage policy for the asset being transferred
     */
    Result<ResourceManifest> generateConsumerResourceManifest(TransferProcess transferProcess, Policy policy);

    /**
     * Generates a resource manifest for a provider-side transfer process. Operations must be idempotent.
     *
     * @param transferProcess  the transfer process
     * @param assetAddress     the asset data address
     * @param policy           the contract agreement usage policy for the asset being transferred
     */
    ResourceManifest generateProviderResourceManifest(TransferProcess transferProcess, DataAddress assetAddress, Policy policy);
}
