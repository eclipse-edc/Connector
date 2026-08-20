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

package org.eclipse.edc.participantcontext.spi.config.store;

import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class ParticipantContextConfigStoreTestBase {

    protected abstract ParticipantContextConfigStore getStore();

    @Test
    protected void save() {

        var config = config();

        getStore().save(config);

        assertThat(getStore().get("participant1"))
                .isNotNull()
                .satisfies(cfg -> {
                    assertThat(cfg.getEntries()).containsAllEntriesOf(config.getEntries());
                    assertThat(cfg.getPrivateEntries()).containsAllEntriesOf(config.getPrivateEntries());
                });

    }

    @Test
    protected void update() {

        var config = config();

        getStore().save(config);

        assertThat(getStore().get("participant1"))
                .isNotNull()
                .satisfies(cfg -> {
                    assertThat(cfg.getEntries()).containsAllEntriesOf(config.getEntries());
                    assertThat(cfg.getPrivateEntries()).containsAllEntriesOf(config.getPrivateEntries());
                });

        var newConfig = config(Map.of("key1", "value1", "key2", "2", "key3", "value3"));

        getStore().save(newConfig);

        assertThat(getStore().get("participant1"))
                .isNotNull()
                .satisfies(cfg -> {
                    assertThat(cfg.getEntries()).containsAllEntriesOf(newConfig.getEntries());
                });

    }

    @Test
    protected void merge_whenNotExisting_shouldCreate() {
        var patch = ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participant1")
                .createdAt(1000)
                .lastModified(1000)
                .entries(Map.of("key1", "value1"))
                .privateEntries(Map.of("sensitive1", "supersecret"))
                .build();

        var merged = getStore().merge(patch);

        assertThat(merged.getEntries()).containsExactlyInAnyOrderEntriesOf(Map.of("key1", "value1"));
        assertThat(merged.getCreatedAt()).isEqualTo(1000);
        assertThat(getStore().get("participant1"))
                .isNotNull()
                .satisfies(cfg -> {
                    assertThat(cfg.getEntries()).containsExactlyInAnyOrderEntriesOf(Map.of("key1", "value1"));
                    assertThat(cfg.getPrivateEntries()).containsExactlyInAnyOrderEntriesOf(Map.of("sensitive1", "supersecret"));
                    assertThat(cfg.getCreatedAt()).isEqualTo(1000);
                });
    }

    @Test
    protected void merge_shouldAddAndOverwriteEntries() {
        getStore().save(config());

        getStore().merge(patch(Map.of("key1", "updated", "key4", "value4"), Map.of()));

        assertThat(getStore().get("participant1")).isNotNull()
                .satisfies(cfg -> assertThat(cfg.getEntries()).containsExactlyInAnyOrderEntriesOf(
                        Map.of("key1", "updated", "key2", "2", "key3", "value3", "key4", "value4")));
    }

    @Test
    protected void merge_shouldRemoveEntry_whenValueIsNull() {
        getStore().save(config());

        var entries = new HashMap<String, String>();
        entries.put("key1", null);
        var privateEntries = new HashMap<String, String>();
        privateEntries.put("sensitive1", null);
        getStore().merge(patch(entries, privateEntries));

        assertThat(getStore().get("participant1")).isNotNull()
                .satisfies(cfg -> {
                    assertThat(cfg.getEntries()).containsExactlyInAnyOrderEntriesOf(Map.of("key2", "2", "key3", "value3"));
                    assertThat(cfg.getPrivateEntries()).containsExactlyInAnyOrderEntriesOf(Map.of("sensitive2", "5"));
                });
    }

    @Test
    protected void merge_whenKeyAbsent_nullIsNoop() {
        getStore().save(config());

        var entries = new HashMap<String, String>();
        entries.put("missing", null);
        getStore().merge(patch(entries, Map.of()));

        assertThat(getStore().get("participant1")).isNotNull()
                .satisfies(cfg -> assertThat(cfg.getEntries()).containsExactlyInAnyOrderEntriesOf(
                        Map.of("key1", "value1", "key2", "2", "key3", "value3")));
    }

    @Test
    protected void merge_shouldNotTouchPrivateEntries_whenPatchingEntriesOnly() {
        getStore().save(config());

        getStore().merge(patch(Map.of("key4", "value4"), Map.of()));

        assertThat(getStore().get("participant1")).isNotNull()
                .satisfies(cfg -> assertThat(cfg.getPrivateEntries()).containsExactlyInAnyOrderEntriesOf(
                        Map.of("sensitive1", "supersecret", "sensitive2", "5")));
    }

    @Test
    protected void merge_shouldPreserveCreatedAt_andUpdateLastModified() {
        getStore().save(ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participant1")
                .createdAt(1000)
                .lastModified(1000)
                .entries(Map.of("key1", "value1"))
                .build());

        getStore().merge(patch(Map.of("key2", "value2"), Map.of()));

        assertThat(getStore().get("participant1")).isNotNull()
                .satisfies(cfg -> {
                    assertThat(cfg.getCreatedAt()).isEqualTo(1000);
                    assertThat(cfg.getLastModified()).isEqualTo(9000);
                });
    }

    @Test
    protected void merge_shouldReturnMergedConfiguration() {
        getStore().save(config());

        var merged = getStore().merge(patch(Map.of("key4", "value4"), Map.of()));

        assertThat(getStore().get("participant1")).isNotNull()
                .satisfies(cfg -> {
                    assertThat(cfg.getEntries()).containsExactlyInAnyOrderEntriesOf(merged.getEntries());
                    assertThat(cfg.getPrivateEntries()).containsExactlyInAnyOrderEntriesOf(merged.getPrivateEntries());
                    assertThat(cfg.getLastModified()).isEqualTo(merged.getLastModified());
                });
    }

    private ParticipantContextConfiguration patch(Map<String, String> entries, Map<String, String> privateEntries) {
        return ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participant1")
                .createdAt(8000)
                .lastModified(9000)
                .entries(entries)
                .privateEntries(privateEntries)
                .build();
    }

    private ParticipantContextConfiguration config() {
        return ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participant1")
                .entries(Map.of("key1", "value1", "key2", "2", "key3", "value3"))
                .privateEntries(Map.of("sensitive1", "supersecret", "sensitive2", "5"))
                .build();
    }

    private ParticipantContextConfiguration config(Map<String, String> cfg) {
        return ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participant1")
                .entries(cfg)
                .build();
    }

    @Test
    void get_whenNotFound() {
        assertThat(getStore().get("participant1")).isNull();
    }

}
