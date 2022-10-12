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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.api.observability;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.spi.system.health.HealthStatus;

@OpenAPIDefinition
@Tag(name = "Application Observability")
public interface ObservabilityApi {

    @Operation(description = "Performs a liveness probe to determine whether the runtime is working properly.",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = HealthStatus.class)))) }
    )
    Response checkHealth();

    @Operation(description = "Performs a liveness probe to determine whether the runtime is working properly.",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = HealthStatus.class)))) }
    )
    Response getLiveness();

    @Operation(description = "Performs a readiness probe to determine whether the runtime is able to accept requests.",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = HealthStatus.class)))) }
    )
    Response getReadiness();

    @Operation(description = "Performs a startup probe to determine whether the runtime has completed startup.",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = HealthStatus.class)))) }
    )
    Response getStartup();

}
