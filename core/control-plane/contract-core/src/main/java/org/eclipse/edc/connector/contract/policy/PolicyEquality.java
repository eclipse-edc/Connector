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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.contract.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.List;
import java.util.function.BiPredicate;

public class PolicyEquality implements BiPredicate<Policy, Policy> {

    private static final List<String> EXCLUDED_PROPERTIES = List.of("@type", "assignee", "target");
    private final ObjectMapper mapper;

    public PolicyEquality(TypeManager typeManager) {
        this.mapper = typeManager.getMapper();
    }

    @Override
    public boolean test(Policy one, Policy two) {
        var oneTree = mapper.<ObjectNode>valueToTree(one);
        var twoTree = mapper.<ObjectNode>valueToTree(two);
        EXCLUDED_PROPERTIES.forEach(property -> {
            oneTree.remove(property);
            twoTree.remove(property);
        });
        return oneTree.equals(twoTree);
    }
}
