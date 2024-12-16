/*
 *  Copyright (c) 2024 Cofinity-X
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

package org.eclipse.edc.web.spi.configuration;

import java.util.List;

/**
 * PortMapping registry.
 */
public interface PortMappings {

    /**
     * Register a PortMapping.
     *
     * @param portMapping the port mapping.
     */
    void register(PortMapping portMapping);

    /**
     * Return all the registered port mapping.
     *
     * @return all the port mappings registered.
     */
    List<PortMapping> getAll();

}
