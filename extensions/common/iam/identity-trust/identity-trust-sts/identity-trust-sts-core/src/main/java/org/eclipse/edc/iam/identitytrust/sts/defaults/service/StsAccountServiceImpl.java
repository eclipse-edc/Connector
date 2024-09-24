/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.sts.defaults.service;

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Optional;

import static java.lang.String.format;

public class StsAccountServiceImpl implements StsAccountService {

    private final StsAccountStore stsAccountStore;
    private final TransactionContext transactionContext;
    private final Vault vault;

    public StsAccountServiceImpl(StsAccountStore stsAccountStore, Vault vault, TransactionContext transactionContext) {
        this.stsAccountStore = stsAccountStore;
        this.vault = vault;
        this.transactionContext = transactionContext;
    }

    @Override
    public ServiceResult<StsAccount> create(StsAccount client) {
        return transactionContext.execute(() -> ServiceResult.from(stsAccountStore.create(client)));
    }

    @Override
    public ServiceResult<StsAccount> findByClientId(String clientId) {
        return transactionContext.execute(() -> ServiceResult.from(stsAccountStore.findByClientId(clientId)));
    }

    @Override
    public ServiceResult<StsAccount> authenticate(StsAccount client, String secret) {
        return Optional.ofNullable(vault.resolveSecret(client.getSecretAlias()))
                .filter(vaultSecret -> vaultSecret.equals(secret))
                .map(s -> ServiceResult.success(client))
                .orElseGet(() -> ServiceResult.unauthorized(format("Failed to authenticate client with id %s", client.getId())));
    }
}
