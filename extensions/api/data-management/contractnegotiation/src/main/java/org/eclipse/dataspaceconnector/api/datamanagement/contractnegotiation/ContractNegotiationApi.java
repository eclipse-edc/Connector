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
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.ContractAgreementDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.ContractNegotiationDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;

import java.util.List;

@OpenAPIDefinition
public interface ContractNegotiationApi {

    List<ContractNegotiationDto> getNegotiations(Integer offset, Integer limit, String filterExpression, SortOrder sortOrder, String sortField);

    ContractNegotiationDto getNegotiation(String id);

    String getNegotiationState(String id);

    ContractAgreementDto getAgreementForNegotiation(String negotiationId);

    String initiateContractNegotiation(NegotiationInitiateRequestDto initiateDto);

    void cancelNegotiation(String id);

    void declineNegotiation(String id);
}
