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

package org.eclipse.edc.policy.engine.spi;

import org.eclipse.edc.policy.model.Rule;

/**
 * Invoked during policy evaluation as when the left operand of an atomic constraint evaluates to a key that is not bound to a {@link AtomicConstraintFunction}.
 * The function is responsible for performing policy evaluation on the right operand and the left operand.
 *
 * @deprecated use {@link DynamicAtomicConstraintRuleFunction}
 */
@Deprecated(since = "0.10.0")
public interface DynamicAtomicConstraintFunction<R extends Rule> extends DynamicAtomicConstraintRuleFunction<R, PolicyContext> {

}
