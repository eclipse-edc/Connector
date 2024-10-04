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

/**
 * A {@link Policy} validator that can be registered in the {@link PolicyEngine} in pre- or post-evaluation phase.
 *
 * @deprecated use {@link PolicyValidatorRule}
 */
@Deprecated(since = "0.10.0")
@FunctionalInterface
public interface PolicyValidatorFunction extends PolicyValidatorRule<PolicyContext> {

}
