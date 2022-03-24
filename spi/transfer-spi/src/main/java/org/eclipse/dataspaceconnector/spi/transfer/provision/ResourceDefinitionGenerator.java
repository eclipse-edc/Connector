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
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.Nullable;

/**
 * Generates a resource definition for a data transfer request.
 */
public interface ResourceDefinitionGenerator {

    /**
     * Generates a resource definition. If no resource definition is generated, return null.
     *
     * @param process the transfer process to generate the definition for
     * @param policy the contract agreement usage policy for the asset being transferred
     */
    @Nullable
    ResourceDefinition generate(TransferProcess process, Policy policy);

}
