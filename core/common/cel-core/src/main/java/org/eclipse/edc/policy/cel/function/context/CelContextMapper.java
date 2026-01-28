/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.policy.cel.function.context;

import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.spi.result.Result;

import java.util.Map;

/**
 * Supplies context data for CEL expression evaluation based on the provided policy context.
 *
 * @param <C> the type of PolicyContext
 */
public interface CelContextMapper<C extends PolicyContext> {

    Result<Map<String, Object>> mapContext(C context);

}
