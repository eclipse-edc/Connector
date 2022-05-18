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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.eclipse.dataspaceconnector.api.datamanagement.contractagreement.model.ContractAgreementDto;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;

import java.util.List;

@OpenAPIDefinition
@Tag(name = "Contract Agreement")
public interface ContractAgreementApi {

    List<ContractAgreementDto> getAllAgreements(@Valid QuerySpecDto querySpecDto);

    ContractAgreementDto getContractAgreement(String id);

}
