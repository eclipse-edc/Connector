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

package org.eclipse.edc.connector.transfer.spi.provision;

import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.jetbrains.annotations.Nullable;

/**
 * Generates a resource definition for a data transfer request on a consumer.
 */
public interface ConsumerResourceDefinitionGenerator {

    /**
     * Generates a ResourceDefinition. If no resource definition is generated, return null.
     *
     * @param transferProcess the transfer process
     * @param policy          the contract agreement usage policy for the asset being transferred
     */
    @Nullable
    ResourceDefinition generate(TransferProcess transferProcess, Policy policy);

    /**
     * checks if a ResourceDefinition can be generated
     *
     * @param transferProcess the transfer process
     * @param policy          the contract agreement usage policy for the asset being transferred
     */

    boolean canGenerate(TransferProcess transferProcess, Policy policy);

}
