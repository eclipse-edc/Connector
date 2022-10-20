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

package org.eclipse.edc.connector.transfer.spi.provision;

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ResourceManifest;
import org.eclipse.edc.policy.engine.spi.PolicyScope;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * Generates resource manifests for data transfer requests. Implementations are responsible for enforcing policy constraints associated with transfer requests.
 */
@ExtensionPoint
public interface ResourceManifestGenerator {

    @PolicyScope
    String MANIFEST_VERIFICATION_SCOPE = "provision.manifest.verify";

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
    Result<ResourceManifest> generateConsumerResourceManifest(DataRequest dataRequest, Policy policy);

    /**
     * Generates a resource manifest for a provider-side data request. Operations must be idempotent.
     *
     * @param dataRequest  the data request associated with transfer process
     * @param assetAddress the asset data address
     * @param policy       the contract agreement usage policy for the asset being transferred
     */
    ResourceManifest generateProviderResourceManifest(DataRequest dataRequest, DataAddress assetAddress, Policy policy);
}
