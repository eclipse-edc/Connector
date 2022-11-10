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

package org.eclipse.edc.connector.transfer.spi.provision;

import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.Nullable;

/**
 * Generates a resource definition for a data transfer request on a provider.
 */
public interface ProviderResourceDefinitionGenerator {

    /**
     * Generates a ResourceDefinition. If no resource definition is generated, return null.
     *
     * @param dataRequest  the data request associated with transfer process
     * @param assetAddress the asset data address
     * @param policy       the contract agreement usage policy for the asset being transferred
     */
    @Nullable
    ResourceDefinition generate(DataRequest dataRequest, DataAddress assetAddress, Policy policy);

    /**
     * checks if a ResourceDefinition can be generated
     *
     * @param dataRequest  the data request associated with transfer process
     * @param assetAddress the asset data address
     * @param policy       the contract agreement usage policy for the asset being transferred
     */

    boolean canGenerate(DataRequest dataRequest, DataAddress assetAddress, Policy policy);

}
