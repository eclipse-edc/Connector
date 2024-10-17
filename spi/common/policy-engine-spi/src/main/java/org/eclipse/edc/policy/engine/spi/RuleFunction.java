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
 * Invoked during policy evaluation to examine a rule node.
 *
 * @deprecated use {@link PolicyRuleFunction}.
 */
@Deprecated(since = "0.10.0")
public interface RuleFunction<R extends Rule> extends PolicyRuleFunction<R, PolicyContext> {

}
