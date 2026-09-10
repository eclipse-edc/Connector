/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.participantcontext.connector.profile;

import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.edc.protocol.spi.ParticipantProfileService.PROFILES_CONFIG_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParticipantProfileServiceImplTest {

    private static final JsonLdNamespace NAMESPACE = new JsonLdNamespace("https://example.org/dspace/");

    private final ParticipantContextConfigStore configStore = mock();
    private final DataspaceProfileContextRegistry registry = mock();
    private final TransactionContext transactionContext = new NoopTransactionContext();

    private final ParticipantProfileServiceImpl resolver = new ParticipantProfileServiceImpl(configStore, registry, transactionContext, false);

    @Test
    void resolveAll_returnsProfilesInConfigOrder() {
        when(configStore.get("p1")).thenReturn(config("p1", "a, b , c"));
        var pa = profile("a");
        var pb = profile("b");
        var pc = profile("c");
        when(registry.getProfile("a")).thenReturn(pa);
        when(registry.getProfile("b")).thenReturn(pb);
        when(registry.getProfile("c")).thenReturn(pc);

        assertThat(resolver.resolveAll("p1")).containsExactly(pa, pb, pc);
    }

    @Test
    void resolveAll_skipsUnknownIds() {
        when(configStore.get("p1")).thenReturn(config("p1", "known,unknown"));
        var known = profile("known");
        when(registry.getProfile("known")).thenReturn(known);
        when(registry.getProfile("unknown")).thenReturn(null);

        assertThat(resolver.resolveAll("p1")).containsExactly(known);
    }

    @Test
    void resolveAll_emptyConfig_returnsEmpty() {
        when(configStore.get("p1")).thenReturn(config("p1", ""));

        assertThat(resolver.resolveAll("p1")).isEmpty();
    }

    @Test
    void resolveAll_blankAndDuplicateEntries_areNormalized() {
        when(configStore.get("p1")).thenReturn(config("p1", ",a,,a,b,"));
        var pa = profile("a");
        var pb = profile("b");
        when(registry.getProfile("a")).thenReturn(pa);
        when(registry.getProfile("b")).thenReturn(pb);

        assertThat(resolver.resolveAll("p1")).containsExactly(pa, pb);
    }

    @Test
    void resolve_associatedAndRegistered_returnsProfile() {
        when(configStore.get("p1")).thenReturn(config("p1", "a,b"));
        var pb = profile("b");
        when(registry.getProfile("b")).thenReturn(pb);

        assertThat(resolver.resolve("p1", "b")).isEqualTo(pb);
    }

    @Test
    void resolve_notAssociated_returnsEmpty() {
        when(configStore.get("p1")).thenReturn(config("p1", "a"));

        assertThat(resolver.resolve("p1", "b")).isNull();
    }

    @Test
    void resolve_associatedButUnregistered_returnsEmpty() {
        when(configStore.get("p1")).thenReturn(config("p1", "a,b"));
        when(registry.getProfile("b")).thenReturn(null);

        assertThat(resolver.resolve("p1", "b")).isNull();
    }

    @Test
    void associateProfiles_validProfiles_mergesCsvAndReturnsSuccess() {
        when(registry.getProfile("a")).thenReturn(profile("a"));
        when(registry.getProfile("b")).thenReturn(profile("b"));

        var result = resolver.associateProfiles("p1", List.of("a", "b"));

        assertThat(result.succeeded()).isTrue();
        assertThat(capturePatch())
                .satisfies(patch -> assertThat(patch.getParticipantContextId()).isEqualTo("p1"))
                .satisfies(patch -> assertThat(patch.getEntries()).containsOnly(entry(PROFILES_CONFIG_KEY, "a,b")));
        verify(configStore, never()).save(any());
    }

    @Test
    void associateProfiles_shouldNotRewriteUnrelatedEntries() {
        when(registry.getProfile("a")).thenReturn(profile("a"));

        var result = resolver.associateProfiles("p1", List.of("a"));

        assertThat(result.succeeded()).isTrue();
        var patch = capturePatch();
        // the patch must carry the profiles key alone: anything else would clobber entries written concurrently
        assertThat(patch.getEntries()).containsOnlyKeys(PROFILES_CONFIG_KEY);
        assertThat(patch.getPrivateEntries()).isEmpty();
        // reading the stored configuration first would reintroduce the read-modify-write race
        verify(configStore, never()).get(any());
        verify(configStore, never()).save(any());
    }

    @Test
    void associateProfiles_unknownProfile_returnsBadRequestAndDoesNotSave() {
        when(registry.getProfile("a")).thenReturn(profile("a"));
        when(registry.getProfile("unknown")).thenReturn(null);

        var result = resolver.associateProfiles("p1", List.of("a", "unknown"));

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(m -> m.contains("Profile unknown does not exist"));
        verify(configStore, never()).get(any());
        verify(configStore, never()).merge(any());
    }

    @Test
    void associateProfiles_multipleUnknownProfiles_aggregatesAllViolations() {
        when(registry.getProfile("x")).thenReturn(null);
        when(registry.getProfile("y")).thenReturn(null);

        var result = resolver.associateProfiles("p1", List.of("x", "y"));

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages())
                .anyMatch(m -> m.contains("Profile x does not exist"))
                .anyMatch(m -> m.contains("Profile y does not exist"));
        verify(configStore, never()).merge(any());
    }

    @Test
    void associateProfiles_emptyList_writesEmptyEntryAndReturnsSuccess() {
        var result = resolver.associateProfiles("p1", List.of());

        assertThat(result.succeeded()).isTrue();
        assertThat(capturePatch().getEntries()).containsOnly(entry(PROFILES_CONFIG_KEY, ""));
    }

    @Test
    void associateProfiles_configNotExisting_delegatesCreationToStore() {
        when(registry.getProfile("a")).thenReturn(profile("a"));

        var result = resolver.associateProfiles("p1", List.of("a"));

        assertThat(result.failed()).isFalse();
        // the store creates the configuration from the patch when none exists yet
        verify(configStore).merge(any());
    }


    private ParticipantContextConfiguration capturePatch() {
        var captor = ArgumentCaptor.forClass(ParticipantContextConfiguration.class);
        verify(configStore).merge(captor.capture());
        return captor.getValue();
    }

    private ParticipantContextConfiguration config(String participantContextId, String profiles) {

        var entries = new HashMap<String, String>();
        entries.put(PROFILES_CONFIG_KEY, profiles);
        return ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId(participantContextId)
                .entries(entries)
                .build();
    }

    private DataspaceProfileContext profile(String id) {
        return new DataspaceProfileContext(id, new ProtocolVersion("v", "/v", "https"),
                () -> "url", ct -> "id", NAMESPACE, List.of(), List.of());
    }
}
