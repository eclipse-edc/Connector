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

package org.eclipse.edc.participantcontext.config.store;

import org.eclipse.edc.participantcontext.single.config.store.SingleParticipantContextConfigStore;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStoreTestBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SingleParticipantContextConfigStoreTest extends ParticipantContextConfigStoreTestBase {

    public static final String PARTICIPANT_CONTEXT_ID = "configuredId";
    private final ParticipantContextConfiguration config = ParticipantContextConfiguration.Builder.newInstance()
            .participantContextId(PARTICIPANT_CONTEXT_ID)
            .entries(Map.of("key", "value"))
            .build();

    private final SingleParticipantContextConfigStore store = new SingleParticipantContextConfigStore(config);

    @Override
    protected ParticipantContextConfigStore getStore() {
        return store;
    }

    // SingleParticipantContextConfigStore is read-only
    @Override
    @Disabled
    protected void save() {
        super.save();
    }

    // SingleParticipantContextConfigStore is read-only
    @Override
    @Disabled
    protected void update() {
        super.update();
    }

    @Test
    void get() {
        assertThat(getStore().get(PARTICIPANT_CONTEXT_ID))
                .isNotNull()
                .satisfies(cfg -> {
                    assertThat(cfg.getEntries()).containsAllEntriesOf(config.getEntries());
                });
    }

    @Test
    void save_unsupported() {

        assertThatThrownBy(() -> getStore().save(config))
                .isInstanceOf(UnsupportedOperationException.class);
    }

}
