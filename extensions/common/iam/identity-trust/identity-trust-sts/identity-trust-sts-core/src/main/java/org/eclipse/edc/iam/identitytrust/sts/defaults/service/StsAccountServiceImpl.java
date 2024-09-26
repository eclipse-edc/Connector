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
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientSecretGenerator;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.spi.result.ServiceResult.from;

public class StsAccountServiceImpl implements StsAccountService {

    private final StsAccountStore stsAccountStore;
    private final TransactionContext transactionContext;
    private final Vault vault;
    private final StsClientSecretGenerator stsClientSecretGenerator;

    public StsAccountServiceImpl(StsAccountStore stsAccountStore, Vault vault, TransactionContext transactionContext, StsClientSecretGenerator stsClientSecretGenerator) {
        this.stsAccountStore = stsAccountStore;
        this.vault = vault;
        this.transactionContext = transactionContext;
        this.stsClientSecretGenerator = stsClientSecretGenerator;
    }

    @Override
    public ServiceResult<StsAccount> create(StsAccount client) {
        return create(client, null)
                .map(newSecret -> client);
    }

    @Override
    public ServiceResult<String> create(StsAccount client, @Nullable String clientSecret) {
        var result = transactionContext.execute(() -> from(stsAccountStore.create(client)));
        clientSecret = ofNullable(clientSecret).orElseGet(this::generateSecret);
        if (result.succeeded()) {
            var vaultResult = vault.storeSecret(client.getSecretAlias(), clientSecret);
            if (vaultResult.failed()) {
                return ServiceResult.unexpected("Error storing client secret. Manual intervention is required for alias %s. %s".formatted(client.getSecretAlias(), vaultResult.getFailureDetail()));
            }
            return ServiceResult.success(clientSecret);
        }
        return result.mapFailure();
    }

    @Override
    public ServiceResult<StsAccount> findByClientId(String clientId) {
        return transactionContext.execute(() -> from(stsAccountStore.findByClientId(clientId)));
    }

    @Override
    public ServiceResult<Void> update(StsAccount client) {
        return transactionContext.execute(() -> from(stsAccountStore.update(client)));
    }

    @Override
    public ServiceResult<String> updateSecret(String id, String newSecretAlias, @Nullable String newSecret) {
        Objects.requireNonNull(newSecretAlias);

        var oldAlias = new AtomicReference<String>();
        // generate new secret if needed
        newSecret = ofNullable(newSecret).orElseGet(this::generateSecret);

        var updateResult = transactionContext.execute(() -> stsAccountStore.findById(id)
                .compose(stsAccount -> {
                    oldAlias.set(stsAccount.getSecretAlias());
                    stsAccount.updateSecretAlias(newSecretAlias);
                    return stsAccountStore.update(stsAccount);
                }));

        if (updateResult.succeeded()) {
            var oldSecretAlias = oldAlias.get();
            Result<Void> vaultInteractionResult = Result.success();

            if (!oldSecretAlias.equals(newSecretAlias)) {
                vaultInteractionResult = vaultInteractionResult.merge(vault.deleteSecret(oldSecretAlias));
            }

            var finalNewSecret = newSecret;
            vaultInteractionResult = vaultInteractionResult.compose(v -> vault.storeSecret(newSecretAlias, finalNewSecret));
            return vaultInteractionResult.succeeded()
                    ? ServiceResult.success(newSecretAlias)
                    : ServiceResult.unexpected(vaultInteractionResult.getFailureDetail());
        }
        return ServiceResult.fromFailure(updateResult);
    }

    @Override
    public ServiceResult<Void> deleteById(String id) {
        return transactionContext.execute(() -> ServiceResult.from(stsAccountStore.deleteById(id))
                .onSuccess(stsAccount -> vault.deleteSecret(stsAccount.getSecretAlias()))
                .mapEmpty());
    }

    @Override
    public Collection<StsAccount> queryAccounts(QuerySpec querySpec) {
        return transactionContext.execute(() -> stsAccountStore.findAll(querySpec).toList());
    }

    @Override
    public ServiceResult<StsAccount> authenticate(StsAccount client, String secret) {
        return ofNullable(vault.resolveSecret(client.getSecretAlias()))
                .filter(vaultSecret -> vaultSecret.equals(secret))
                .map(s -> ServiceResult.success(client))
                .orElseGet(() -> ServiceResult.unauthorized(format("Failed to authenticate client with id %s", client.getId())));
    }

    @Override
    public ServiceResult<StsAccount> findById(String accountId) {
        return transactionContext.execute(() -> from(stsAccountStore.findById(accountId)));
    }

    private String generateSecret() {
        return stsClientSecretGenerator.generateClientSecret(null);
    }
}
