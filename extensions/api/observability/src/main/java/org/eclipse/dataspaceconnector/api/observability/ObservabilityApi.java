/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.dataspaceconnector.api.observability;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import jakarta.ws.rs.core.Response;

@OpenAPIDefinition
public interface ObservabilityApi {

    Response checkHealth();

    Response getLiveness();

    Response getReadiness();

    Response getStartup();

}
