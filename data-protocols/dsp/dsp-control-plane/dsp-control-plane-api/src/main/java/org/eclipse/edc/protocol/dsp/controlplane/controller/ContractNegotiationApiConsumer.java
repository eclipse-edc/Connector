/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.controlplane.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonObject;

@OpenAPIDefinition
@Tag(name = "Dataspace Protocol: Contract Negotiation")
public interface ContractNegotiationApiConsumer {

    @Operation(description = "Adds contract offer to contract negotiation", operationId = "dspProviderOfferNegotiation")
    void providerOffer(String id, JsonObject body);

    @Operation(description = "Creates an agreement within a contract negotiation", operationId = "dspCreateAgreementNegotiation")
    void createAgreement(String id, JsonObject body);

    @Operation(description = "Finalizes an agreement within a contract negotiation", operationId = "dspFinalizeAgreementNegotiation")
    void finalizeAgreement(String id, JsonObject body);
}
