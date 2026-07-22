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

package org.eclipse.edc.policy.cel.function;

import java.util.List;
import java.util.function.Function;

/**
 * A single custom function overload that can be made available to CEL expressions.
 * <p>
 * Multiple {@link CelFunction}s may share the same {@link #name()} to declare several overloads of the same function;
 * the {@link #overloadId()} must be unique across all registered functions.
 * <p>
 * For member (receiver style) functions the first entry of {@link #argumentTypes()} is the receiver type, so that
 * {@code vc.withType('X')} is declared with argument types {@code [LIST, STRING]}.
 *
 * @param name           the function name as used in expressions, e.g. {@code withType}
 * @param overloadId     globally unique identifier of this overload, e.g. {@code vc_list_with_type}
 * @param memberFunction whether the function is invoked receiver style ({@code a.f(b)}) or globally ({@code f(a, b)})
 * @param resultType     the type this function evaluates to
 * @param argumentTypes  the argument types; for member functions the first entry is the receiver
 * @param implementation the function body, receiving arguments in declaration order
 */
public record CelFunction(String name,
                          String overloadId,
                          boolean memberFunction,
                          CelValueType resultType,
                          List<CelValueType> argumentTypes,
                          Function<List<Object>, Object> implementation) {

    public CelFunction {
        argumentTypes = List.copyOf(argumentTypes);
        if (memberFunction && argumentTypes.isEmpty()) {
            throw new IllegalArgumentException("A member function requires at least the receiver argument type");
        }
    }
}
