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
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.secret.Secret;

import java.util.List;
import java.util.Optional;

public class SecretServiceImpl implements SecretService {
    private final Vault vault;
    private final SecretObservable observable;

    public SecretServiceImpl(Vault vault, SecretObservable observable) {
        this.vault = vault;
        this.observable = observable;
    }

    @Override
    public Secret findById(String secretId) {
        return Optional.ofNullable(vault.resolveSecret(secretId))
                .map(secretValue -> Secret.Builder.newInstance()
                        .value(secretValue)
                        .id(secretId)
                        .build())
                .orElse(null);
    }

    @Override
    public ServiceResult<List<Secret>> search(QuerySpec query) {
        throw new UnsupportedOperationException("Query operation is not supported for secrets");
    }

    @Override
    public ServiceResult<Secret> create(Secret secret) {
        var existingSecret = findById(secret.getId());

        if (existingSecret != null) {
            return ServiceResult.conflict("Secret " + secret.getId() + " already exist");
        }

        var createResult = vault.storeSecret(secret.getId(), secret.getValue());
        if (createResult.succeeded()) {
            observable.invokeForEach(l -> l.created(secret));
            return ServiceResult.success(secret);
        }

        return ServiceResult.badRequest(createResult.getFailureMessages().toString());
    }

    @Override
    public ServiceResult<Secret> delete(String secretKey) {
        var existingSecret = findById(secretKey);

        if (existingSecret == null) {
            return ServiceResult.notFound("Secret " + secretKey + " not found");
        }

        var deleteResult = vault.deleteSecret(secretKey);
        if (deleteResult.succeeded()) {
            deleteResult.onSuccess(a -> observable.invokeForEach(l -> l.deleted(secretKey)));
            return ServiceResult.success(null);
        }
        return ServiceResult.badRequest(deleteResult.getFailureMessages().toString());
    }

    @Override
    public ServiceResult<Secret> update(Secret secret) {
        var existingSecret = findById(secret.getId());

        if (existingSecret == null) {
            return ServiceResult.notFound("Secret " + secret.getId() + " not found");
        }

        var updateResult = vault.storeSecret(secret.getId(), secret.getValue());
        if (updateResult.succeeded()) {
            observable.invokeForEach(l -> l.updated(secret));
            return ServiceResult.success(secret);
        }
        return ServiceResult.badRequest(updateResult.getFailureMessages().toString());
    }

}

