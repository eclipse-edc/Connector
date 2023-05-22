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

package org.eclipse.edc.transform.spi;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A context used to perform recursive transformation of a type tree.If a problem is reported, the transformation will fail.
 */
public interface TransformerContext {

    /**
     * Returns if problems have been reported during the transformation.
     */
    boolean hasProblems();

    /**
     * Returns a list of reported problems or an empty collection.
     */
    List<String> getProblems();

    /**
     * Reports a problem.
     * <p>
     * Note {@link #problem()} should be used in most cases.
     */
    void reportProblem(String problem);

    /**
     * Returns a problem builder that can be used to report problems in a typed manner.
     * <p>
     * Note this method should be preferred to reporting untyped problems using {@link #reportProblem(String)}.
     */
    ProblemBuilder problem();

    /**
     * Transforms the input object and any contained types, returning its transformed representation or null if the
     * operation cannot be completed or input is null.
     *
     * @param input    the instance to transform
     * @param <INPUT>  the instance type
     * @param <OUTPUT> the transformed object type
     * @return the transformed representation or null
     */
    @Nullable <INPUT, OUTPUT> OUTPUT transform(INPUT input, Class<OUTPUT> outputType);

    /**
     * Returns a registered type alias for the schema type.
     */
    @Nullable
    default Class<?> typeAlias(String type) {
        return null;
    }

    /**
     * Returns a registered type alias for the schema type or the default alias if none is registered.
     */
    default Class<?> typeAlias(String type, Class<?> defaultType) {
        return defaultType;
    }
}
