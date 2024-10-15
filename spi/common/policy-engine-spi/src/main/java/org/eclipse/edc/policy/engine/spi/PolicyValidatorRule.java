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

import org.eclipse.edc.policy.model.Policy;

import java.util.function.BiFunction;

/**
 * A {@link Policy} validator that can be registered in the {@link PolicyEngine} in pre- or post-evaluation phase.
 */
@FunctionalInterface
public interface PolicyValidatorRule<C extends PolicyContext> extends BiFunction<Policy, C, Boolean> {

    /**
     * Returns the name of the {@link PolicyValidatorRule}
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
