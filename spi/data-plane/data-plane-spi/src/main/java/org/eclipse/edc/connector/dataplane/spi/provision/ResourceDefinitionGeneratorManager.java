/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.spi.provision;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;

import java.util.List;
import java.util.Set;

/**
 * Generates resource definitions for data flow requests.
 * Implementations are responsible for enforcing policy constraints associated with transfer requests.
 */
public interface ResourceDefinitionGeneratorManager {

    /**
     * Registers a consumer-side provisioner.
     *
     * @param generator the generator
     */
    void registerConsumerGenerator(ResourceDefinitionGenerator generator);

    /**
     * Registers a provider-side provisioner.
     *
     * @param generator the generator
     */
    void registerProviderGenerator(ResourceDefinitionGenerator generator);

    /**
     * Generates resource definitions for a consumer-side transfer. Operations must be idempotent.
     *
     * @param dataFlow the data flow
     * @return succeeded if generation went through, failure otherwise
     */
    List<ProvisionResource> generateConsumerResourceDefinition(DataFlow dataFlow);

    /**
     * Generates resource definitions for a provider-side transfer. Operations must be idempotent.
     *
     * @param dataFlow the data flow
     * @return succeeded if generation went through, failure otherwise
     */
    List<ProvisionResource> generateProviderResourceDefinition(DataFlow dataFlow);

    /**
     * Return a set of the supported destination types
     *
     * @return supported destination types.
     */
    Set<String> destinationTypes();
}
