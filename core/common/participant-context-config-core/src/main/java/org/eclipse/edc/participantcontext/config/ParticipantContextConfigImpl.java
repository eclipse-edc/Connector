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

package org.eclipse.edc.participantcontext.config;

import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.transaction.spi.TransactionContext;

public class ParticipantContextConfigImpl implements ParticipantContextConfig {


    private final ParticipantContextConfigStore configStore;
    private final TransactionContext transactionContext;

    public ParticipantContextConfigImpl(ParticipantContextConfigStore configStore, TransactionContext transactionContext) {
        this.configStore = configStore;
        this.transactionContext = transactionContext;
    }

    @Override
    public String getString(String participantContextId, String key) {
        return config(participantContextId).getString(key);
    }

    @Override
    public String getString(String participantContextId, String key, String defaultValue) {
        return config(participantContextId).getString(key, defaultValue);
    }

    @Override
    public Integer getInteger(String participantContextId, String key) {
        return config(participantContextId).getInteger(key);
    }

    @Override
    public Integer getInteger(String participantContextId, String key, Integer defaultValue) {
        return config(participantContextId).getInteger(key, defaultValue);
    }

    @Override
    public Long getLong(String participantContextId, String key) {
        return config(participantContextId).getLong(key);

    }

    @Override
    public Long getLong(String participantContextId, String key, Long defaultValue) {
        return config(participantContextId).getLong(key, defaultValue);
    }

    @Override
    public Boolean getBoolean(String participantContextId, String key) {
        return config(participantContextId).getBoolean(key);
    }

    @Override
    public Boolean getBoolean(String participantContextId, String key, Boolean defaultValue) {
        return config(participantContextId).getBoolean(key, defaultValue);
    }

    private Config config(String participantContextId) {
        return transactionContext.execute(() -> {
            var cfg = configStore.get(participantContextId);
            if (cfg == null) {
                throw new EdcException("No configuration found for participant context " + participantContextId);
            }
            return ConfigFactory.fromMap(cfg.getEntries());
        });
    }

}
