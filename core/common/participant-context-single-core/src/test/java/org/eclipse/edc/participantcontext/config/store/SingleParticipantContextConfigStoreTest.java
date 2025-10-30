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
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStoreTestBase;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SingleParticipantContextConfigStoreTest extends ParticipantContextConfigStoreTestBase {

    public static final String PARTICIPANT_CONTEXT_ID = "configuredId";
    private final Config config = ConfigFactory.fromMap(Map.of("key", "value"));
    private final SingleParticipantContextConfigStore store = new SingleParticipantContextConfigStore(PARTICIPANT_CONTEXT_ID, config);

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

        assertThatThrownBy(() -> getStore().save("participant1", config))
                .isInstanceOf(UnsupportedOperationException.class);
    }

}
