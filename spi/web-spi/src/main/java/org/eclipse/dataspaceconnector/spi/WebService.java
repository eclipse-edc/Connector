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

package org.eclipse.dataspaceconnector.spi;

import org.eclipse.dataspaceconnector.spi.system.Feature;

/**
 * Manages the runtime web (HTTP) service.
 */
@Feature("edc:core:base:webservice")
public interface WebService {

    /**
     * Registers a JAX-RS resource instance, or controller. Extensions may contribute bespoke APIs to the runtime.
     *
     * @deprecated Use {@link WebService#registerResource(Object)} instead, as it is actually a resource we're registering.
     */
    @Deprecated
    void registerController(Object controller);

    void registerResource(Object controller);

    /**
     * Registers a resource (e.g. a controller or a filter) with the webservice, making it only available for
     * the port mapping that matches the {@code contextAlias} parameter.
     * <p>
     * Note that registering a resource for a context alias, for which no port mapping exists, may raise an exception when
     * starting the WebService.
     *
     * @param contextAlias a String identifying the respective port mapping.
     * @param resource     a resource
     */
    void registerResource(String contextAlias, Object resource);
}
