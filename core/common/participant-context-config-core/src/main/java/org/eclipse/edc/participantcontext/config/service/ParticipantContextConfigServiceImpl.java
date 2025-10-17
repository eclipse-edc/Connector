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

import org.eclipse.edc.participantcontext.spi.config.service.ParticipantContextConfigService;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.transaction.spi.TransactionContext;

public class ParticipantContextConfigServiceImpl implements ParticipantContextConfigService {

    private final ParticipantContextConfigStore configStore;
    private final TransactionContext transactionContext;

    public ParticipantContextConfigServiceImpl(ParticipantContextConfigStore configStore, TransactionContext transactionContext) {
        this.configStore = configStore;
        this.transactionContext = transactionContext;
    }

    @Override
    public ServiceResult<Void> save(String participantContextId, Config config) {
        return transactionContext.execute(() -> {
            configStore.save(participantContextId, config);
            return ServiceResult.success();
        });
    }

    @Override
    public ServiceResult<Config> get(String participantContextId) {
        return transactionContext.execute(() -> {
            var config = configStore.get(participantContextId);
            if (config == null) {
                return ServiceResult.notFound("No configuration found for participant context with id " + participantContextId);
            }
            return ServiceResult.success(config);
        });
    }
}
