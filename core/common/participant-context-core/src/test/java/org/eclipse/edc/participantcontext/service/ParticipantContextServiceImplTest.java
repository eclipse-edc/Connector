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

import org.assertj.core.api.Assertions;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParticipantContextServiceImplTest {

    private final ParticipantContextStore participantContextStore = mock();
    private ParticipantContextServiceImpl participantContextService;

    @BeforeEach
    void setUp() {
        participantContextService = new ParticipantContextServiceImpl(participantContextStore, new NoopTransactionContext());
    }

    @Test
    void createParticipantContextParticipantContext_storageFails() {
        when(participantContextStore.create(any())).thenReturn(StoreResult.duplicateKeys("foobar"));

        var ctx = createParticipantContextContext();
        assertThat(participantContextService.createParticipantContext(ctx))
                .isFailed();

        verify(participantContextStore).create(any());
    }

    @Test
    void createParticipantContextParticipantContext_whenExists() {
        when(participantContextStore.create(any())).thenReturn(StoreResult.alreadyExists("test-failure"));

        var ctx = createParticipantContextContext();
        assertThat(participantContextService.createParticipantContext(ctx))
                .isFailed()
                .satisfies(f -> Assertions.assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.CONFLICT));
        verify(participantContextStore).create(any());

    }

    @Test
    void getParticipantContext() {
        var ctx = createParticipantContextContext();
        when(participantContextStore.findById(any())).thenReturn(StoreResult.success(ctx));

        assertThat(participantContextService.getParticipantContext("test-id"))
                .isSucceeded()
                .usingRecursiveComparison()
                .isEqualTo(ctx);

        verify(participantContextStore).findById(anyString());
    }

    @Test
    void getParticipantContext_whenNotExists() {
        when(participantContextStore.findById(anyString())).thenReturn(StoreResult.notFound("foo"));
        assertThat(participantContextService.getParticipantContext("test-id"))
                .isFailed()
                .satisfies(f -> {
                    Assertions.assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND);
                    Assertions.assertThat(f.getFailureDetail()).isEqualTo("foo");
                });

        verify(participantContextStore).findById(anyString());
    }

    @Test
    void getParticipantContext_whenStorageFails() {
        when(participantContextStore.findById(anyString())).thenReturn(StoreResult.notFound("foo bar"));
        assertThat(participantContextService.getParticipantContext("test-id"))
                .isFailed()
                .satisfies(f -> {
                    Assertions.assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND);
                    Assertions.assertThat(f.getFailureDetail()).isEqualTo("foo bar");
                });

        verify(participantContextStore).findById(anyString());
    }

    @Test
    void deleteParticipantContextParticipantContext() {
        when(participantContextStore.findById(anyString())).thenReturn(StoreResult.success(createParticipantContextContext()));
        when(participantContextStore.deleteById(anyString())).thenReturn(StoreResult.success());
        when(participantContextStore.update(any())).thenReturn(StoreResult.success());
        assertThat(participantContextService.deleteParticipantContext("test-id")).isSucceeded();

        verify(participantContextStore).deleteById(anyString());
    }


    @Test
    void deleteParticipantContextParticipantContext_whenNotExists() {
        when(participantContextStore.findById(anyString())).thenReturn(StoreResult.success(createParticipantContextContext()));
        when(participantContextStore.deleteById(any())).thenReturn(StoreResult.notFound("foo bar"));
        when(participantContextStore.update(any())).thenReturn(StoreResult.success());

        assertThat(participantContextService.deleteParticipantContext("test-id"))
                .isFailed()
                .satisfies(f -> {
                    Assertions.assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND);
                    Assertions.assertThat(f.getFailureDetail()).isEqualTo("foo bar");
                });

        verify(participantContextStore).deleteById(anyString());
    }


    @Test
    void update() {
        var context = createParticipantContextContext();
        when(participantContextStore.update(any())).thenReturn(StoreResult.success());
        assertThat(participantContextService.updateParticipantContext(context)).isSucceeded();

        verify(participantContextStore).update(any());
    }

    @Test
    void update_whenStoreUpdateFails() {
        var context = createParticipantContextContext();
        when(participantContextStore.update(any())).thenReturn(StoreResult.alreadyExists("test-msg"));

        assertThat(participantContextService.updateParticipantContext(context)).isFailed()
                .detail().isEqualTo("test-msg");

        verify(participantContextStore).update(any());
    }

    @Test
    void query() {
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(List.of(
                createParticipantContextContext(),
                createParticipantContextContext(),
                createParticipantContextContext())));

        assertThat(participantContextService.search(QuerySpec.max()))
                .isSucceeded()
                .satisfies(res -> Assertions.assertThat(res).hasSize(3));

        verify(participantContextStore).query(any());
    }

    private ParticipantContext createParticipantContextContext() {
        return ParticipantContext.Builder.newInstance().participantContextId("test-id").identity("test-id").build();
    }

}
