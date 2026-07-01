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

import org.eclipse.edc.encryption.EncryptionAlgorithmRegistry;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.service.ParticipantContextConfigService;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ParticipantContextConfigServiceImplTest {

    private final ParticipantContextConfigStore store = mock();

    private final EncryptionAlgorithmRegistry registry = mock();

    private final Clock clock = Clock.fixed(Instant.ofEpochMilli(5000), ZoneId.systemDefault());

    private final ParticipantContextConfigService service = new ParticipantContextConfigServiceImpl(registry, "any", store, new NoopTransactionContext(), clock);


    @Test
    void save() {

        when(registry.encrypt(anyString(), anyString())).then(a -> Result.success(a.getArgument(1)));

        var cfg = ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participantContext")
                .entries(Map.of("key", "value"))
                .privateEntries(Map.of("key", "private"))
                .build();

        var result = service.save(cfg);
        assertThat(result).isSucceeded();

        verify(store).save(argThat(saved ->
                saved.getParticipantContextId().equals(cfg.getParticipantContextId()) &&
                        saved.getEntries().equals(cfg.getEntries()) &&
                        saved.getPrivateEntries().equals(cfg.getPrivateEntries())));
        verify(registry).encrypt(anyString(), anyString());
    }

    @Test
    void merge() {
        when(registry.encrypt(anyString(), anyString())).then(a -> Result.success("enc(" + a.getArgument(1) + ")"));

        var existing = ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participantContext")
                .createdAt(1000)
                .lastModified(1000)
                .entries(new HashMap<>(Map.of("key", "value", "keep", "kept")))
                .privateEntries(new HashMap<>(Map.of("secret", "enc(existing)")))
                .build();
        when(store.get("participantContext")).thenReturn(existing);

        var patch = ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participantContext")
                .entries(Map.of("key", "updated", "new", "added"))
                .privateEntries(Map.of("newSecret", "plain"))
                .build();

        var result = service.merge(patch);
        assertThat(result).isSucceeded();

        verify(store).save(argThat(saved ->
                saved.getParticipantContextId().equals("participantContext") &&
                        saved.getCreatedAt() == 1000 &&
                        saved.getLastModified() == 5000 &&
                        saved.getEntries().equals(Map.of("key", "updated", "keep", "kept", "new", "added")) &&
                        saved.getPrivateEntries().equals(Map.of("secret", "enc(existing)", "newSecret", "enc(plain)"))));
        // only the patch's private entry is encrypted, existing ones are left untouched
        verify(registry).encrypt(anyString(), anyString());
    }

    @Test
    void merge_whenNotFound_shouldCreate() {
        when(registry.encrypt(anyString(), anyString())).then(a -> Result.success("enc(" + a.getArgument(1) + ")"));
        when(store.get("participantContext")).thenReturn(null);

        var patch = ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participantContext")
                .entries(Map.of("key", "value"))
                .privateEntries(Map.of("secret", "plain"))
                .build();

        var result = service.merge(patch);
        assertThat(result).isSucceeded();

        verify(store).save(argThat(saved ->
                saved.getParticipantContextId().equals("participantContext") &&
                        saved.getCreatedAt() == 5000 &&
                        saved.getLastModified() == 5000 &&
                        saved.getEntries().equals(Map.of("key", "value")) &&
                        saved.getPrivateEntries().equals(Map.of("secret", "enc(plain)"))));
        verify(registry).encrypt(anyString(), anyString());
    }

    @Test
    void merge_shouldRemoveEntry_whenValueIsNull() {
        when(registry.encrypt(anyString(), anyString())).then(a -> Result.success("enc(" + a.getArgument(1) + ")"));

        var existing = ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participantContext")
                .createdAt(1000)
                .lastModified(1000)
                .entries(new HashMap<>(Map.of("key", "value", "keep", "kept")))
                .privateEntries(new HashMap<>(Map.of("secret", "enc(existing)", "keepSecret", "enc(kept)")))
                .build();
        when(store.get("participantContext")).thenReturn(existing);

        var entries = new HashMap<String, String>();
        entries.put("key", null);
        var privateEntries = new HashMap<String, String>();
        privateEntries.put("secret", null);
        var patch = ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participantContext")
                .entries(entries)
                .privateEntries(privateEntries)
                .build();

        var result = service.merge(patch);
        assertThat(result).isSucceeded();

        verify(store).save(argThat(saved ->
                saved.getEntries().equals(Map.of("keep", "kept")) &&
                        saved.getPrivateEntries().equals(Map.of("keepSecret", "enc(kept)"))));
        // null values are removal signals and must never be encrypted
        verify(registry, never()).encrypt(anyString(), any());
    }

    @Test
    void merge_whenKeyAbsent_nullIsNoop() {
        when(registry.encrypt(anyString(), anyString())).then(a -> Result.success("enc(" + a.getArgument(1) + ")"));

        var existing = ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participantContext")
                .entries(new HashMap<>(Map.of("keep", "kept")))
                .build();
        when(store.get("participantContext")).thenReturn(existing);

        var entries = new HashMap<String, String>();
        entries.put("missing", null);
        var patch = ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participantContext")
                .entries(entries)
                .build();

        var result = service.merge(patch);
        assertThat(result).isSucceeded();

        verify(store).save(argThat(saved -> saved.getEntries().equals(Map.of("keep", "kept"))));
    }

    @Test
    void save_shouldReturnBadRequest_whenNullValue() {
        var entries = new HashMap<String, String>();
        entries.put("key", null);
        var config = ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participantContext")
                .entries(entries)
                .build();

        var result = service.save(config);

        assertThat(result).isFailed().detail().contains("Null values are not allowed");
        verify(store, never()).save(any());
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
