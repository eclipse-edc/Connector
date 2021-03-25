package com.microsoft.dagx.spi.transfer.provision;

import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;

/**
 * Generates a resource definition for a data transfer request.
 */
public interface ResourceDefinitionGenerator {

    /**
     * Generates the resource definition.
     */
    ResourceDefinition generate(DataRequest request);

}
