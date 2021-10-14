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
package org.eclipse.dataspaceconnector.ids.spi.transform;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * The result of a transformation.
 */
public class TransformResult<OUTPUT> {
    private final List<String> problems;
    private final OUTPUT output;

    public TransformResult(List<String> problems) {
        this.output = null;
        this.problems = problems;
    }

    public TransformResult(OUTPUT output) {
        this.output = output;
        problems = emptyList();
    }

    /**
     * Returns if problems have been problems reported during the transformation.
     */
    public boolean hasProblems() {
        return !problems.isEmpty();
    }

    /**
     * Returns a list of reported problems or an empty collection.
     */
    public List<String> getProblems() {
        return problems;
    }


    /**
     * Returns the transformed type or null if there were problems.
     */
    @Nullable
    public OUTPUT getOutput() {
        return output;
    }

}
