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
package org.eclipse.dataspaceconnector.spi.policy;

import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;

/**
 * Manages rule bindings to policy scopes.
 */
public interface RuleBindingRegistry {

    /**
     * Binds a rule type to the scope. A rule type has two manifestations: (1) The type of {@link Action} specified by a rule; or (2) The left-hand operand of an
     * {@link AtomicConstraint} contained in the rule.
     */
    void bind(String ruleType, String scope);

    /**
     * Returns true of the rule type is bound to the scope; otherwise false.
     */
    boolean isInScope(String ruleType, String scope);
}
