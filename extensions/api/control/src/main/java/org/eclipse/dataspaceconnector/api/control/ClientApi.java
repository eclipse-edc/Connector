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

package org.eclipse.dataspaceconnector.api.control;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

@Deprecated
@OpenAPIDefinition
public interface ClientApi {

    Response addTransfer(DataRequest dataRequest);

    Response initiateNegotiation(ContractOfferRequest contractOffer);

    Response getNegotiationById(String id);

    Response getNegotiationStateById(String id);

    Response getAgreementById(String id);
}
