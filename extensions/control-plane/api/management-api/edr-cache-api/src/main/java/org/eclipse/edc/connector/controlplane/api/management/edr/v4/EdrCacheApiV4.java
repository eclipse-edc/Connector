/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.edr.v4;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.api.model.ApiCoreSchema;

@OpenAPIDefinition(info = @Info(version = "v4alpha"))
@Tag(name = "EDR Cache v4alpha")
public interface EdrCacheApiV4 {

    @Operation(description = "Request all Edr entries according to a particular query",
            requestBody = @RequestBody(
                    content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.QUERY_SPEC))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The edr entries matching the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.EDR_ENTRY)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            })
    JsonArray requestEdrEntriesV4(JsonObject querySpecJson);

    @Operation(description = "Gets the EDR data address with the given transfer process ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The data address",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.DATA_ADDRESS))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "An EDR data address with the given transfer process ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    JsonObject getEdrEntryDataAddressV4(String transferProcessId);

    @Operation(description = "Removes an EDR entry given the transfer process ID",
            responses = {
                    @ApiResponse(responseCode = "204", description = "EDR entry was deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "An EDR entry with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            })
    void removeEdrEntryV4(String transferProcessId);

}
