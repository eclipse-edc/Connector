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

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.ContractAgreementDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.ContractNegotiationDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.NegotiationId;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.NegotiationState;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;

import java.util.List;

@OpenAPIDefinition
@Tag(name = "Contract Negotiation")
public interface ContractNegotiationApi {

    List<ContractNegotiationDto> getNegotiations(@Valid QuerySpecDto querySpecDto);

    ContractNegotiationDto getNegotiation(String id);

    NegotiationState getNegotiationState(String id);

    ContractAgreementDto getAgreementForNegotiation(String negotiationId);

    NegotiationId initiateContractNegotiation(@Valid NegotiationInitiateRequestDto initiateDto);

    void cancelNegotiation(String id);

    void declineNegotiation(String id);
}
