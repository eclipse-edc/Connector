/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.dataplane.api.controller;

import jakarta.ws.rs.container.ContainerRequestContext;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * This class provides a set of API wrapping a {@link ContainerRequestContext}.
 */
public interface ContainerRequestContextApi {
    /**
     * Return the value of the auth header provided in the request.
     */
    @Nullable
    String authHeader(ContainerRequestContext context);

    /**
     * Map the properties (query params, method...) of the request into a readable map.
     */
    Map<String, String> properties(ContainerRequestContext context);
}
