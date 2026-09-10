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

package org.eclipse.edc.participantcontext.spi.config.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParticipantContextConfigurationTest {

    @Test
    void mergeOnto_shouldAddAndOverwriteEntries() {
        var base = config(Map.of("a", "1", "b", "2"), Map.of("s1", "x"), 1000, 1000);
        var patch = config(Map.of("b", "9", "c", "3"), Map.of("s2", "y"), 4000, 5000);

        var merged = patch.mergeOnto(base);

        assertThat(merged.getEntries()).containsExactlyInAnyOrderEntriesOf(Map.of("a", "1", "b", "9", "c", "3"));
        assertThat(merged.getPrivateEntries()).containsExactlyInAnyOrderEntriesOf(Map.of("s1", "x", "s2", "y"));
    }

    @Test
    void mergeOnto_shouldRemoveEntry_whenValueIsNull() {
        var base = config(Map.of("a", "1", "b", "2"), Map.of("s1", "x", "s2", "y"), 1000, 1000);

        var entries = new HashMap<String, String>();
        entries.put("b", null);
        var privateEntries = new HashMap<String, String>();
        privateEntries.put("s1", null);
        var patch = config(entries, privateEntries, 4000, 5000);

        var merged = patch.mergeOnto(base);

        assertThat(merged.getEntries()).containsExactlyInAnyOrderEntriesOf(Map.of("a", "1"));
        assertThat(merged.getPrivateEntries()).containsExactlyInAnyOrderEntriesOf(Map.of("s2", "y"));
    }

    @Test
    void mergeOnto_whenKeyAbsent_nullIsNoop() {
        var base = config(Map.of("a", "1"), Map.of(), 1000, 1000);

        var entries = new HashMap<String, String>();
        entries.put("missing", null);
        var merged = config(entries, Map.of(), 4000, 5000).mergeOnto(base);

        assertThat(merged.getEntries()).containsExactlyInAnyOrderEntriesOf(Map.of("a", "1"));
    }

    @Test
    void mergeOnto_shouldMergeEntriesAndPrivateEntriesIndependently() {
        var base = config(Map.of("a", "1"), Map.of("s1", "x"), 1000, 1000);

        var merged = config(Map.of("b", "2"), Map.of(), 4000, 5000).mergeOnto(base);

        // patching only the public entries must not wipe the private ones
        assertThat(merged.getEntries()).containsExactlyInAnyOrderEntriesOf(Map.of("a", "1", "b", "2"));
        assertThat(merged.getPrivateEntries()).containsExactlyInAnyOrderEntriesOf(Map.of("s1", "x"));
    }

    @Test
    void mergeOnto_whenBaseIsNull_shouldCreateFromPatch() {
        var patch = config(Map.of("a", "1"), Map.of("s1", "x"), 4000, 5000);

        var merged = patch.mergeOnto(null);

        assertThat(merged.getParticipantContextId()).isEqualTo("participant1");
        assertThat(merged.getCreatedAt()).isEqualTo(4000);
        assertThat(merged.getLastModified()).isEqualTo(5000);
        assertThat(merged.getEntries()).containsExactlyInAnyOrderEntriesOf(Map.of("a", "1"));
        assertThat(merged.getPrivateEntries()).containsExactlyInAnyOrderEntriesOf(Map.of("s1", "x"));
    }

    @Test
    void mergeOnto_shouldKeepBaseCreatedAt_andTakePatchLastModified() {
        var base = config(Map.of(), Map.of(), 1000, 2000);

        var merged = config(Map.of(), Map.of(), 4000, 5000).mergeOnto(base);

        assertThat(merged.getCreatedAt()).isEqualTo(1000);
        assertThat(merged.getLastModified()).isEqualTo(5000);
    }

    @Test
    void mergeOnto_shouldNotAliasTheInputMaps() {
        var base = config(new HashMap<>(Map.of("a", "1")), new HashMap<>(Map.of("s1", "x")), 1000, 1000);
        var patch = config(new HashMap<>(Map.of("b", "2")), new HashMap<>(Map.of("s2", "y")), 4000, 5000);

        var merged = patch.mergeOnto(base);

        // the entry maps are not writable through the accessors at all, so no result can be mutated into an input
        assertThatThrownBy(() -> merged.getEntries().put("c", "3")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> merged.getPrivateEntries().put("s3", "z")).isInstanceOf(UnsupportedOperationException.class);

        assertThat(base.getEntries()).containsOnlyKeys("a");
        assertThat(patch.getEntries()).containsOnlyKeys("b");
    }

    @Test
    void builder_shouldCopyTheGivenMaps() {
        var entries = new HashMap<>(Map.of("a", "1"));
        var privateEntries = new HashMap<>(Map.of("s1", "x"));

        var config = config(entries, privateEntries, 1000, 1000);

        // a caller holding on to the map it passed in must not be able to reach into the configuration afterwards
        entries.put("b", "2");
        privateEntries.put("s2", "y");

        assertThat(config.getEntries()).containsOnlyKeys("a");
        assertThat(config.getPrivateEntries()).containsOnlyKeys("s1");
    }

    @Test
    void toBuilder_shouldCopyTheEntryMaps() {
        var original = config(new HashMap<>(Map.of("a", "1")), new HashMap<>(Map.of("s1", "x")), 1000, 1000);

        var copy = original.toBuilder().entry("b", "2").privateEntry("s2", "y").build();

        assertThat(copy.getEntries()).containsOnlyKeys("a", "b");
        assertThat(original.getEntries()).containsOnlyKeys("a");
        assertThat(copy.getPrivateEntries()).containsOnlyKeys("s1", "s2");
        assertThat(original.getPrivateEntries()).containsOnlyKeys("s1");
    }

    private ParticipantContextConfiguration config(Map<String, String> entries, Map<String, String> privateEntries,
                                                   long createdAt, long lastModified) {
        return ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participant1")
                .createdAt(createdAt)
                .lastModified(lastModified)
                .entries(entries)
                .privateEntries(privateEntries)
                .build();
    }
}
