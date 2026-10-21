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

package org.eclipse.edc.participantcontext.service;

import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Collection;

import static org.eclipse.edc.participantcontext.spi.types.ParticipantContextState.ACTIVATED;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantContextState.DEACTIVATED;

public record ParticipantContextServiceImpl(
        ParticipantContextStore participantContextStore,
        TransactionContext transactionContext) implements ParticipantContextService {


    @Override
    public ServiceResult<ParticipantContext> createParticipantContext(ParticipantContext participantContext) {
        return transactionContext.execute(() -> participantContextStore.create(participantContext)
                .flatMap(ServiceResult::from).map(it -> participantContext));
    }

    @Override
    public ServiceResult<ParticipantContext> getParticipantContext(String participantContextId) {
        return transactionContext.execute(() -> participantContextStore.findById(participantContextId)
                .flatMap(ServiceResult::from));
    }

    @Override
    public ServiceResult<Void> updateParticipantContext(ParticipantContext participantContext) {
        return transactionContext.execute(() -> participantContextStore.findById(participantContext.getId())
                .flatMap(ServiceResult::from)
                .compose(stored -> update(participantContext.toBuilder().state(stored.getStateAsEnum()).build())));
    }

    @Override
    public ServiceResult<Void> activateParticipantContext(String participantContextId) {
        return changeState(participantContextId, ACTIVATED);
    }

    @Override
    public ServiceResult<Void> deactivateParticipantContext(String participantContextId) {
        return changeState(participantContextId, DEACTIVATED);
    }

    @Override
    public ServiceResult<Void> deleteParticipantContext(String participantContextId) {
        return transactionContext.execute(() -> participantContextStore.deleteById(participantContextId)
                .flatMap(ServiceResult::from));
    }

    @Override
    public ServiceResult<Collection<ParticipantContext>> search(QuerySpec query) {
        return transactionContext.execute(() -> participantContextStore.query(query)
                .flatMap(ServiceResult::from));
    }

    private ServiceResult<Void> changeState(String participantContextId, ParticipantContextState state) {
        return transactionContext.execute(() -> participantContextStore.findById(participantContextId)
                .flatMap(ServiceResult::from)
                .compose(stored -> update(stored.toBuilder().state(state).build())));
    }

    private ServiceResult<Void> update(ParticipantContext participantContext) {
        return ServiceResult.from(participantContextStore.update(participantContext));
    }
}
