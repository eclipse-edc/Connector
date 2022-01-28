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
     */
    void registerController(Object controller);

}
