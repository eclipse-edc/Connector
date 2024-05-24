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

package org.eclipse.edc.connector.controlplane.api.management.contractagreement.v2;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Path;
import org.eclipse.edc.api.ApiWarnings;
import org.eclipse.edc.connector.controlplane.api.management.contractagreement.BaseContractAgreementApiController;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

@Path("/v2/contractagreements")
public class ContractAgreementApiV2Controller extends BaseContractAgreementApiController implements ContractAgreementApiV2 {
    public ContractAgreementApiV2Controller(ContractAgreementService service, TypeTransformerRegistry transformerRegistry, Monitor monitor, JsonObjectValidatorRegistry validatorRegistry) {
        super(service, transformerRegistry, monitor, validatorRegistry);
    }

    @Override
    public JsonArray queryAgreements(JsonObject querySpecJson) {
        return super.queryAgreements(querySpecJson);
    }

    @Override
    public JsonObject getAgreementById(String id) {
        monitor.warning(ApiWarnings.deprecationWarning("/v2", "/v3"));
        return super.getAgreementById(id);
    }

    @Override
    public JsonObject getNegotiationByAgreementId(String id) {
        monitor.warning(ApiWarnings.deprecationWarning("/v2", "/v3"));
        return super.getNegotiationByAgreementId(id);
    }
}
