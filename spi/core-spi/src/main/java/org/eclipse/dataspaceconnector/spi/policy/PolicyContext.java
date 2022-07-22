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
 *       Fraunhofer Institute for Software and Systems Engineering - context data
 *
 */

package org.eclipse.dataspaceconnector.spi.policy;

import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
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
     * Returns the participant agent to evaluate the policy against.
     */
    ParticipantAgent getParticipantAgent();
    
    /**
     * Gets additional data from the context by type.
     *
     * @param type the type class.
     * @param <T> the type of data.
     * @return the object associated with the type, or null.
     */
    <T> T getContextData(Class<T> type);
    
    /**
     * Adds additional data to the context.
     *
     * @param type the type class.
     * @param data the data.
     * @param <T> the type of data.
     */
    <T> void putContextData(Class<T> type, T data);
    
}
