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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - context data
 *
 */

package org.eclipse.edc.policy.engine.spi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Context used during policy evaluation.
 */
public interface PolicyContext {

    /**
     * Reports a problem during evaluation.
     */
    void reportProblem(String problem);

    /**
     * Returns true if problems were reported.
     */
    boolean hasProblems();

    /**
     * Returns problems reported during policy evaluation or an empty collection.
     */
    @NotNull
    List<String> getProblems();

    /**
     * The policy scope
     *
     * @return the policy scope.
     */
    String scope();

}
