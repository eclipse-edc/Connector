/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.policy.cel.function.context;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractDefinitionPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes the {@link ContractDefinition} of a {@link ContractDefinitionPolicyContext} as {@code ctx.contractDefinition},
 * with its {@code id}, {@code participantContextId}, {@code accessPolicyId}, {@code contractPolicyId},
 * {@code privateProperties} and {@code assetsSelector} (a list of {@code {operandLeft, operator, operandRight}}).
 * Contexts that do not carry a contract definition get no {@code contractDefinition} entry.
 */
public class ContractDefinitionContextMapper implements CelContextMapper<PolicyContext> {

    public static final String CONTRACT_DEFINITION = "contractDefinition";

    @Override
    public Result<Map<String, Object>> mapContext(PolicyContext context) {
        if (!(context instanceof ContractDefinitionPolicyContext definitionContext) || definitionContext.contractDefinition() == null) {
            return Result.success(Map.of());
        }
        var definition = definitionContext.contractDefinition();
        var map = new HashMap<String, Object>();
        putIfNotNull(map, "id", definition.getId());
        putIfNotNull(map, "participantContextId", definition.getParticipantContextId());
        putIfNotNull(map, "accessPolicyId", definition.getAccessPolicyId());
        putIfNotNull(map, "contractPolicyId", definition.getContractPolicyId());
        map.put("privateProperties", definition.getPrivateProperties() == null ? Map.of() : definition.getPrivateProperties());
        map.put("assetsSelector", toSelector(definition.getAssetsSelector()));
        return Result.success(Map.of(CONTRACT_DEFINITION, map));
    }

    private List<Map<String, Object>> toSelector(List<Criterion> assetsSelector) {
        if (assetsSelector == null) {
            return List.of();
        }
        return assetsSelector.stream()
                .map(criterion -> {
                    var map = new HashMap<String, Object>();
                    putIfNotNull(map, "operandLeft", criterion.getOperandLeft());
                    putIfNotNull(map, "operator", criterion.getOperator());
                    putIfNotNull(map, "operandRight", criterion.getOperandRight());
                    return (Map<String, Object>) map;
                })
                .toList();
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
