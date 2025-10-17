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

import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class ParticipantContextConfigStoreTestBase {

    protected abstract ParticipantContextConfigStore getStore();

    @Test
    protected void save() {

        var config = ConfigFactory.fromMap(Map.of("key1", "value1", "key2", "2"));

        getStore().save("participant1", config);

        assertThat(getStore().get("participant1"))
                .isNotNull()
                .satisfies(cfg -> {
                    assertThat(cfg.getEntries()).containsAllEntriesOf(cfg.getEntries());
                });


    }

    @Test
    void get_whenNotFound() {
        assertThat(getStore().get("participant1")).isNull();
    }

}
