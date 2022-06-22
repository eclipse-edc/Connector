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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractagreement;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.eclipse.dataspaceconnector.api.datamanagement.contractagreement.model.ContractAgreementDto;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;

import java.util.List;

@OpenAPIDefinition
@Tag(name = "Contract Agreement")
public interface ContractAgreementApi {

    @Operation(description = "Gets all contract agreements according to a particular query",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ContractAgreementDto.class)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed")
            }
    )
    List<ContractAgreementDto> getAllAgreements(@Valid QuerySpecDto querySpecDto);

    @Operation(description = "Gets an contract agreement with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract agreement", content = @Content(schema = @Schema(implementation = ContractAgreementDto.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null"),
                    @ApiResponse(responseCode = "404", description = "An contract agreement with the given ID does not exist")
            }
    )
    ContractAgreementDto getContractAgreement(String id);

}
