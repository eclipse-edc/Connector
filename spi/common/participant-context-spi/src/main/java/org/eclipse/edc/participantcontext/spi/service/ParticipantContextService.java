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

package org.eclipse.edc.participantcontext.spi.service;

import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Collection;


public interface ParticipantContextService {

    /**
     * Creates a new ParticipantContext.
     *
     * @param participantContext The ParticipantContext to create.
     * @return The created ParticipantContext or a failure if one with the same ID already exists.
     */
    ServiceResult<ParticipantContext> createParticipantContext(ParticipantContext participantContext);


    /**
     * Updates an existing ParticipantContext.
     *
     * @param participantContext The ParticipantContext to update.
     * @return The updated ParticipantContext or a failure if not found.
     */
    ServiceResult<Void> updateParticipantContext(ParticipantContext participantContext);

    /**
     * Finds a ParticipantContext by its ID.
     *
     * @param participantContextId The ID of the ParticipantContext to find.
     * @return The found ParticipantContext or a failure if not found.
     */
    ServiceResult<ParticipantContext> getParticipantContext(String participantContextId);

    /**
     * Deletes a ParticipantContext by its ID.
     *
     * @param participantContextId The ID of the ParticipantContext to delete.
     * @return The deleted ParticipantContext or a failure if not found.
     */
    ServiceResult<Void> deleteParticipantContext(String participantContextId);

    /**
     * Searches for ParticipantContexts based on the provided QuerySpec.
     *
     * @param query The QuerySpec defining the search criteria.
     * @return A list of ParticipantContexts matching the query or a failure if the query is invalid.
     */
    ServiceResult<Collection<ParticipantContext>> search(QuerySpec query);

}
