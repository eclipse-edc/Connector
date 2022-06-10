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

import java.util.Map;

/**
 * Wrapper around {@link ContainerRequestContext} enabling mocking.
 */
public interface ContainerRequestContextApi {

    Map<String, String> headers(ContainerRequestContext context);

    String queryParams(ContainerRequestContext context);

    String body(ContainerRequestContext context);

    String path(ContainerRequestContext context);

    String mediaType(ContainerRequestContext context);

    String method(ContainerRequestContext context);
}
