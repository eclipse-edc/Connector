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

package org.eclipse.edc.connector.controlplane.api.management.contractdefinition.v2;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Path;
import org.eclipse.edc.api.ApiWarnings;
import org.eclipse.edc.connector.controlplane.api.management.contractdefinition.BaseContractDefinitionApiController;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

@Path("/v2/contractdefinitions")
public class ContractDefinitionApiV2Controller extends BaseContractDefinitionApiController {
    public ContractDefinitionApiV2Controller(TypeTransformerRegistry transformerRegistry, ContractDefinitionService service, Monitor monitor, JsonObjectValidatorRegistry validatorRegistry) {
        super(transformerRegistry, service, monitor, validatorRegistry);
    }

    @Override
    public JsonObject createContractDefinition(JsonObject createObject) {
        monitor.warning(ApiWarnings.deprecationWarning("/v2", "/v3"));
        return super.createContractDefinition(createObject);
    }
}
