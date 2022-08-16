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

package org.eclipse.dataspaceconnector.spi.transformer;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A context used to report problems and perform recursive transformation of a type tree. If a problem is reported, the transformation will fail.
 */
public interface TransformerContext {

    /**
     * Returns if problems have been problems reported during the transformation.
     */
    boolean hasProblems();

    /**
     * Returns a list of reported problems or an empty collection.
     */
    List<String> getProblems();

    /**
     * Reports a problem.
     */
    void reportProblem(String problem);

    /**
     * Transforms the object and any contained types, returning its transformed representation or null if the operation cannot be completed.
     *
     * @param object   the instance to transform
     * @param <INPUT>  the instance type
     * @param <OUTPUT> the transformed object type
     * @return the transformed representation or null
     */
    @Nullable <INPUT, OUTPUT> OUTPUT transform(INPUT object, Class<OUTPUT> outputType);
}
