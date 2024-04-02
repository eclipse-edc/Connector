/*
 *  Copyright (c) 2024 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.controlplane.services.secret;

import org.eclipse.edc.connector.secret.spi.observe.SecretObservable;
import org.eclipse.edc.connector.spi.service.SecretService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.secret.Secret;

import java.util.List;

public class SecretServiceImpl implements SecretService {
    private final Vault vault;
    private final SecretObservable observable;

    // TODO: check is secret is referenced by an asset (privateProperties)?
    // -> no, use Vault & observable

    public SecretServiceImpl(Vault vault, SecretObservable observable) {
        this.vault = vault;
        this.observable = observable;
    }

    @Override
    public Secret findById(String secretKey) {
        var secretValue = vault.resolveSecret(secretKey);
        // TODO: check if secretValue is null
        return Secret.Builder.newInstance()
                .value(secretValue)
                .key(secretKey)
                .build();
    }

    @Override
    public ServiceResult<List<Secret>> search(QuerySpec query) {
        // TODO: should this function be implemented? The vault doesn't seem to support searching for secrets
        return null;
    }

    @Override
    public ServiceResult<Secret> create(Secret secret) {
        var createResult = vault.storeSecret(secret.getKey(), secret.getValue());
        if (createResult.succeeded()) {
            observable.invokeForEach(l -> l.created(secret));
            return ServiceResult.success(secret);
        }
        StoreResult<Secret> storeResult = StoreResult.notFound("Secret " + secret.getKey() + " not found");
        return ServiceResult.fromFailure(storeResult);
    }

    @Override
    public ServiceResult<Secret> delete(String secretKey) {
        var deleteResult = vault.deleteSecret(secretKey);
        if (deleteResult.succeeded()) {
            StoreResult<Secret> storeResult = StoreResult.success(null);
            storeResult.onSuccess(a -> observable.invokeForEach(l -> l.deleted(a)));
            return ServiceResult.from(storeResult);
        }
        StoreResult<Secret> storeResult = StoreResult.notFound("Secret " + secretKey + " not found");
        return ServiceResult.from(storeResult);
    }

    @Override
    // TODO: should we check if the secret already exist and reject update if not?
    public ServiceResult<Secret> update(Secret secret) {
        var updateResult = vault.storeSecret(secret.getKey(), secret.getValue());
        if (updateResult.succeeded()) {
            observable.invokeForEach(l -> l.updated(secret));
            return ServiceResult.success(secret);
        }
        StoreResult<Secret> storeResult = StoreResult.notFound("Secret " + secret.getKey() + " not found");
        return ServiceResult.fromFailure(storeResult);
    }
}
