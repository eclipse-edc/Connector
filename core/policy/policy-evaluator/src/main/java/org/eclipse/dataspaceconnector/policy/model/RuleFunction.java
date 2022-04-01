/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.policy.model;

/**
 * An extension point that evaluates an {@link Rule} node.
 */
@FunctionalInterface
public interface RuleFunction<RULE_TYPE extends Rule> {

    /**
     * Performs the evaluation.
     */
    boolean evaluate(RULE_TYPE rule);

}
