/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.api.iam.identitytrust.sts;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.BeanParam;
import org.eclipse.edc.api.iam.identitytrust.sts.model.StsTokenErrorResponse;
import org.eclipse.edc.api.iam.identitytrust.sts.model.StsTokenRequest;
import org.eclipse.edc.api.iam.identitytrust.sts.model.StsTokenResponse;

@OpenAPIDefinition
@Tag(name = "Secure Token Service Api")
public interface SecureTokenServiceApi {


    @Operation(description = "",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The Self-Issued ID token",
                            content = @Content(schema = @Schema(implementation = StsTokenResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid Request",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = StsTokenErrorResponse.class))))
            })
    StsTokenResponse token(@BeanParam StsTokenRequest request);

}
