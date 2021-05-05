/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.transfer.provision;

import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.Nullable;

/**
 * Generates a resource definition for a data transfer request.
 */
public interface ResourceDefinitionGenerator {

    /**
     * Generates a resource definition. If no resource definition is generated, return null.
     */
    @Nullable
    ResourceDefinition generate(TransferProcess process);

}
