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
public interface ContractNegotiationApiProvider {
    @Operation(description = "Gets contract negotiation by id", operationId = "dspGetNegotiation")
    JsonObject getNegotiation(String id);

    @Operation(description = "Starts contract negotiation", operationId = "dspInitiateNegotiation")
    JsonObject initiateNegotiation(JsonObject body);

    @Operation(description = "Adds contract offer to contract negotiation", operationId = "dspConsumerOfferNegotiation")
    void consumerOffer(String id, JsonObject body);

    @Operation(description = "Accepts current offer of contract negotiation", operationId = "dspAcceptOfferNegotiation")
    void acceptCurrentOffer(String id, JsonObject body);

    @Operation(description = "Verifies current agreement of contract negotiation", operationId = "dspVerifyAgreementNegotiation")
    void verifyAgreement(String id, JsonObject body);

    @Operation(description = "Terminates contract negotiation", operationId = "dspTerminateNegotiation")
    void terminateNegotiation(String id);
}
