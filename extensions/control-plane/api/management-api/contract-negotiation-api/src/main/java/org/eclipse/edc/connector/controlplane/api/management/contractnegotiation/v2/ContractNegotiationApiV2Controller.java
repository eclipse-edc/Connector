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

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v2;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Path;
import org.eclipse.edc.api.ApiWarnings;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.BaseContractNegotiationApiController;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;


@Path("/v2/contractnegotiations")
public class ContractNegotiationApiV2Controller extends BaseContractNegotiationApiController implements ContractNegotiationApiV2 {
    public ContractNegotiationApiV2Controller(ContractNegotiationService service, TypeTransformerRegistry transformerRegistry, Monitor monitor, JsonObjectValidatorRegistry validatorRegistry) {
        super(service, transformerRegistry, monitor, validatorRegistry);
    }

    @Override
    public JsonArray queryNegotiations(JsonObject querySpecJson) {
        monitor.warning(ApiWarnings.deprecationWarning("/v2", "/v3"));
        return super.queryNegotiations(querySpecJson);
    }

    @Override
    public JsonObject getNegotiation(String id) {
        monitor.warning(ApiWarnings.deprecationWarning("/v2", "/v3"));
        return super.getNegotiation(id);
    }

    @Override
    public JsonObject getNegotiationState(String id) {
        monitor.warning(ApiWarnings.deprecationWarning("/v2", "/v3"));
        return super.getNegotiationState(id);
    }

    @Override
    public JsonObject getAgreementForNegotiation(String negotiationId) {
        monitor.warning(ApiWarnings.deprecationWarning("/v2", "/v3"));
        return super.getAgreementForNegotiation(negotiationId);
    }

    @Override
    public JsonObject initiateContractNegotiation(JsonObject requestObject) {
        monitor.warning(ApiWarnings.deprecationWarning("/v2", "/v3"));
        return super.initiateContractNegotiation(requestObject);
    }

    @Override
    public void terminateNegotiation(String id, JsonObject terminateNegotiation) {
        monitor.warning(ApiWarnings.deprecationWarning("/v2", "/v3"));
        super.terminateNegotiation(id, terminateNegotiation);
    }
}
