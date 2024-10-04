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

package org.eclipse.edc.policy.engine.spi;

import org.eclipse.edc.policy.model.Rule;

/**
 * Invoked during policy evaluation when the left operand of an atomic constraint evaluates to a key associated with this function. The function is responsible for performing
 * policy evaluation on the right operand.
 *
 * @deprecated use {@link AtomicConstraintRuleFunction}.
 */
@Deprecated(since = "0.10.0")
@FunctionalInterface
public interface AtomicConstraintFunction<R extends Rule> extends AtomicConstraintRuleFunction<R, PolicyContext> {

}
