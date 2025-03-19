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

/**
 * Generates a resource definition for a data flow request.
 */
public interface ResourceDefinitionGenerator {

    /**
     * Return the supported DataAddress type.
     *
     * @return supported DataAddress type.
     */
    String supportedType();

    /**
     * Generates a resource definition. If no resource definition is generated, return null.
     *
     * @param dataFlow the data flow
     */
    ProvisionResourceDefinition generate(DataFlow dataFlow);

}
