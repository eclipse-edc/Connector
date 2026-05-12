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
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.spi.ParticipantProfileResolver.PROFILES_CONFIG_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParticipantProfileResolverImplTest {

    private static final JsonLdNamespace NAMESPACE = new JsonLdNamespace("https://example.org/dspace/");

    private final ParticipantContextConfig config = mock();
    private final DataspaceProfileContextRegistry registry = mock();
    private final ParticipantProfileResolverImpl resolver = new ParticipantProfileResolverImpl(config, registry, false);

    @Test
    void resolveAll_returnsProfilesInConfigOrder() {
        when(config.getString(eq("p1"), eq(PROFILES_CONFIG_KEY), any())).thenReturn("a, b , c");
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
        when(config.getString(eq("p1"), eq(PROFILES_CONFIG_KEY), any())).thenReturn("known,unknown");
        var known = profile("known");
        when(registry.getProfile("known")).thenReturn(known);
        when(registry.getProfile("unknown")).thenReturn(null);

        assertThat(resolver.resolveAll("p1")).containsExactly(known);
    }

    @Test
    void resolveAll_emptyConfig_returnsEmpty() {
        when(config.getString(eq("p1"), eq(PROFILES_CONFIG_KEY), any())).thenReturn("");

        assertThat(resolver.resolveAll("p1")).isEmpty();
    }

    @Test
    void resolveAll_blankAndDuplicateEntries_areNormalized() {
        when(config.getString(eq("p1"), eq(PROFILES_CONFIG_KEY), any())).thenReturn(",a,,a,b,");
        var pa = profile("a");
        var pb = profile("b");
        when(registry.getProfile("a")).thenReturn(pa);
        when(registry.getProfile("b")).thenReturn(pb);

        assertThat(resolver.resolveAll("p1")).containsExactly(pa, pb);
    }

    @Test
    void resolve_associatedAndRegistered_returnsProfile() {
        when(config.getString(eq("p1"), eq(PROFILES_CONFIG_KEY), any())).thenReturn("a,b");
        var pb = profile("b");
        when(registry.getProfile("b")).thenReturn(pb);

        assertThat(resolver.resolve("p1", "b")).contains(pb);
    }

    @Test
    void resolve_notAssociated_returnsEmpty() {
        when(config.getString(eq("p1"), eq(PROFILES_CONFIG_KEY), any())).thenReturn("a");

        assertThat(resolver.resolve("p1", "b")).isEmpty();
    }

    @Test
    void resolve_associatedButUnregistered_returnsEmpty() {
        when(config.getString(eq("p1"), eq(PROFILES_CONFIG_KEY), any())).thenReturn("a,b");
        when(registry.getProfile("b")).thenReturn(null);

        assertThat(resolver.resolve("p1", "b")).isEmpty();
    }

    private DataspaceProfileContext profile(String id) {
        return new DataspaceProfileContext(id, new ProtocolVersion("v", "/v", "https"),
                () -> "url", ct -> "id", NAMESPACE, List.of());
    }
}
