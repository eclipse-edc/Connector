/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.version.v1;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.edc.spi.system.apiversion.VersionRecord;

import java.util.List;
import java.util.Map;

@OpenAPIDefinition(info = @Info(
        title = "Version API",
        description = "This contains the version API that provides information about the exact version of the APIs exposed by the runtime"))
@Tag(name = "Version")
public interface VersionApi {

    @Operation(description = "Gets the versions exposed by the runtime", responses = {
            @ApiResponse(responseCode = "200", description = "The version contexts",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = VersionRecord.class))))
    })
    Map<String, List<VersionRecord>> getVersion();

}
