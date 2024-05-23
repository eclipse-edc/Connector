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

package org.eclipse.edc.connector.controlplane.api.management.policy.v2;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Path;
import org.eclipse.edc.api.ApiWarnings;
import org.eclipse.edc.connector.controlplane.api.management.policy.BasePolicyDefinitionApiController;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

@Path("/v2/policydefinitions")
public class PolicyDefinitionApiV2Controller extends BasePolicyDefinitionApiController implements PolicyDefinitionApiV2 {
    public PolicyDefinitionApiV2Controller(Monitor monitor, TypeTransformerRegistry transformerRegistry, PolicyDefinitionService service, JsonObjectValidatorRegistry validatorRegistry) {
        super(monitor, transformerRegistry, service, validatorRegistry);
    }

    @Override
    public JsonArray queryPolicyDefinitions(JsonObject querySpecJson) {
        monitor.warning(ApiWarnings.deprecationWarning("/v2", "/v3"));
        return super.queryPolicyDefinitions(querySpecJson);
    }

    @Override
    public JsonObject getPolicyDefinition(String id) {
        monitor.warning(ApiWarnings.deprecationWarning("/v2", "/v3"));
        return super.getPolicyDefinition(id);
    }

    @Override
    public JsonObject createPolicyDefinition(JsonObject request) {
        monitor.warning(ApiWarnings.deprecationWarning("/v2", "/v3"));
        return super.createPolicyDefinition(request);
    }

    @Override
    public void deletePolicyDefinition(String id) {
        monitor.warning(ApiWarnings.deprecationWarning("/v2", "/v3"));
        super.deletePolicyDefinition(id);
    }

    @Override
    public void updatePolicyDefinition(String id, JsonObject input) {
        monitor.warning(ApiWarnings.deprecationWarning("/v2", "/v3"));
        super.updatePolicyDefinition(id, input);
    }
}
