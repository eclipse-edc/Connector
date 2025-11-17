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

package org.eclipse.edc.participantcontext.config.service;

import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.service.ParticipantContextConfigService;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ParticipantContextConfigServiceImplTest {

    private final ParticipantContextConfigStore store = mock();

    private final ParticipantContextConfigService service = new ParticipantContextConfigServiceImpl(store, new NoopTransactionContext());


    @Test
    void save() {

        var cfg = ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participantContext")
                .build();

        var result = service.save(cfg);
        assertThat(result).isSucceeded();

        verify(store).save(cfg);
    }

    @Test
    void get() {
        var cfg = ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participantContext")
                .build();
        when(store.get("participantContext")).thenReturn(cfg);
        var result = service.get("participantContext");
        assertThat(result).isSucceeded()
                .isEqualTo(cfg);
    }

    @Test
    void get_whenNotFound() {
        var result = service.get("participantContext");
        assertThat(result).isFailed().detail().contains("No configuration found for participant context");
    }
}
