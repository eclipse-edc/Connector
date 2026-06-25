/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.policy.engine;

import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RuleBindingRegistryImpl implements RuleBindingRegistry {
    private static final String DELIMITER = ".";
    private static final String DELIMITED_ALL = "*" + DELIMITER;

    private final Map<String, Set<String>> ruleBindings = new HashMap<>();
    private final List<Function<String, Set<String>>> dynamicBinders = new ArrayList<>();

    @Override
    public void bind(String ruleType, String scope) {
        ruleBindings.computeIfAbsent(ruleType, k -> new HashSet<>()).add(scope + DELIMITER);
    }

    @Override
    public void dynamicBind(Function<String, Set<String>> binder) {
        dynamicBinders.add(binder);
    }

    @Override
    public boolean isInScope(String ruleType, String scope) {
        var boundScopes = bindings(ruleType);
        if (boundScopes.contains(DELIMITED_ALL)) {
            return true;
        }
        var delimitedScope = scope + DELIMITER;
        for (String boundScope : boundScopes) {
            if (delimitedScope.startsWith(boundScope)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> bindings(String ruleType) {
        var boundScopes = ruleBindings.get(ruleType);
        if (boundScopes == null) {
            boundScopes = dynamicBinders.stream()
                    .flatMap(binder -> binder.apply(ruleType).stream())
                    .collect(Collectors.toSet());
        }
        return boundScopes;
    }
}
