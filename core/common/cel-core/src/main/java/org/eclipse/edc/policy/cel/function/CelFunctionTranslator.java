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

import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.CelType;
import dev.cel.common.types.ListType;
import dev.cel.common.types.MapType;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelFunctionBinding;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Translates the EDC-neutral {@link CelFunction} representation into the CEL compiler declarations and runtime
 * bindings.
 */
public class CelFunctionTranslator {

    private CelFunctionTranslator() {
    }

    /**
     * Builds the compiler declarations. All overloads sharing a function name must be declared together, hence the
     * grouping by {@link CelFunction#name()}.
     */
    public static List<CelFunctionDecl> toDeclarations(List<CelFunction> functions) {
        // preserve registration order to keep declarations stable across runs
        var byName = functions.stream()
                .collect(Collectors.groupingBy(CelFunction::name, LinkedHashMap::new, Collectors.toList()));

        return byName.entrySet().stream()
                .map(entry -> CelFunctionDecl.newFunctionDeclaration(entry.getKey(),
                        entry.getValue().stream().map(CelFunctionTranslator::toOverload).toList()))
                .toList();
    }

    /**
     * Builds the runtime bindings, one per overload.
     */
    public static List<CelFunctionBinding> toBindings(List<CelFunction> functions) {
        return functions.stream().map(CelFunctionTranslator::toBinding).toList();
    }

    private static CelOverloadDecl toOverload(CelFunction function) {
        var resultType = toCelType(function.resultType());
        var parameterTypes = function.argumentTypes().stream().map(CelFunctionTranslator::toCelType).toList();

        return function.memberFunction()
                ? CelOverloadDecl.newMemberOverload(function.overloadId(), resultType, parameterTypes)
                : CelOverloadDecl.newGlobalOverload(function.overloadId(), resultType, parameterTypes);
    }

    private static CelFunctionBinding toBinding(CelFunction function) {
        var argClasses = function.argumentTypes().stream().map(CelFunctionTranslator::toRuntimeClass).toList();
        return CelFunctionBinding.from(function.overloadId(), argClasses,
                args -> function.implementation().apply(Arrays.asList(args)));
    }

    private static CelType toCelType(CelValueType type) {
        return switch (type) {
            case STRING -> SimpleType.STRING;
            case BOOL -> SimpleType.BOOL;
            case INT -> SimpleType.INT;
            case DOUBLE -> SimpleType.DOUBLE;
            case LIST -> ListType.create(SimpleType.DYN);
            case MAP -> MapType.create(SimpleType.DYN, SimpleType.DYN);
            case DYN -> SimpleType.DYN;
        };
    }

    /**
     * The runtime dispatches overloads on the concrete Java class of the arguments. {@code DYN} therefore binds to
     * {@link Object}, meaning two overloads of the same arity that differ only in a {@code DYN} position cannot be
     * told apart at runtime.
     */
    private static Class<?> toRuntimeClass(CelValueType type) {
        return switch (type) {
            case STRING -> String.class;
            case BOOL -> Boolean.class;
            case INT -> Long.class;
            case DOUBLE -> Double.class;
            case LIST -> List.class;
            case MAP -> Map.class;
            case DYN -> Object.class;
        };
    }
}
