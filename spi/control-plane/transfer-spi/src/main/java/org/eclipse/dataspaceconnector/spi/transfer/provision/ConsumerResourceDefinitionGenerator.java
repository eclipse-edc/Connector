/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.jetbrains.annotations.Nullable;

/**
 * Generates a resource definition for a data transfer request on a consumer.
 */
public interface ConsumerResourceDefinitionGenerator {

    /**
     * Generates a resource definition. If no resource definition is generated, return null.
     *
     * @param dataRequest the data request associated with transfer process
     * @param policy the contract agreement usage policy for the asset being transferred
     */
    @Nullable
    ResourceDefinition generate(DataRequest dataRequest, Policy policy);

}
