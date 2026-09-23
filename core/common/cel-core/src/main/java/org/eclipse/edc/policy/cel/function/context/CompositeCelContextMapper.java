/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges the output of several {@link CelContextMapper}s into a single CEL context. Mappers are applied in order and
 * a later mapper overrides keys emitted by an earlier one. The first failure aborts the mapping.
 */
public class CompositeCelContextMapper<C extends PolicyContext> implements CelContextMapper<C> {

    private final List<CelContextMapper<? super C>> mappers;

    public CompositeCelContextMapper(List<CelContextMapper<? super C>> mappers) {
        this.mappers = List.copyOf(mappers);
    }

    @SafeVarargs
    public static <C extends PolicyContext> CompositeCelContextMapper<C> of(CelContextMapper<? super C>... mappers) {
        return new CompositeCelContextMapper<C>(List.of(mappers));
    }

    @Override
    public Result<Map<String, Object>> mapContext(C context) {
        var merged = new HashMap<String, Object>();
        for (var mapper : mappers) {
            var result = mapper.mapContext(context);
            if (result.failed()) {
                return result;
            }
            merged.putAll(result.getContent());
        }
        return Result.success(merged);
    }
}
