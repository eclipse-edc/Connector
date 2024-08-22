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

package org.eclipse.edc.policy.engine.spi;

import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

import java.util.Set;
import java.util.function.Function;

/**
 * Manages rule bindings to policy scopes.
 */
@ExtensionPoint
public interface RuleBindingRegistry {

    /**
     * Binds a rule type to the scope. A rule type has two manifestations: (1) The type of {@link Action} specified by a rule; or (2) The left-hand operand of an
     * {@link AtomicConstraint} contained in the rule.
     */
    void bind(String ruleType, String scope);

    /**
     * Register a dynamic binder that will be invoked as fallback if the rule type is not found in the registry
     */
    void dynamicBind(Function<String, Set<String>> binder);

    /**
     * Returns true of the rule type is bound to the scope; otherwise false.
     */
    boolean isInScope(String ruleType, String scope);


    /**
     * Returns the bindings for a rule type;
     */
    Set<String> bindings(String ruleType);
}
