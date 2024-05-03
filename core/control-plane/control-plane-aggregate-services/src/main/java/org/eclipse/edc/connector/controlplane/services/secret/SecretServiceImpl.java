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
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.secret.Secret;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.spi.result.ServiceResult.badRequest;
import static org.eclipse.edc.spi.result.ServiceResult.conflict;
import static org.eclipse.edc.spi.result.ServiceResult.notFound;
import static org.eclipse.edc.spi.result.ServiceResult.success;

public class SecretServiceImpl implements SecretService {
    private final Vault vault;
    private final SecretObservable observable;

    public SecretServiceImpl(Vault vault, SecretObservable observable) {
        this.vault = vault;
        this.observable = observable;
    }

    @Override
    public Secret findById(String secretId) {
        return ofNullable(vault.resolveSecret(secretId))
                .map(secretValue -> Secret.Builder.newInstance()
                        .value(secretValue)
                        .id(secretId)
                        .build())
                .orElse(null);
    }

    @Override
    public ServiceResult<Secret> create(Secret secret) {
        var existing = findById(secret.getId());
        if (existing != null) {
            return conflict("Secret " + secret.getId() + " already exist");
        }

        return vault.storeSecret(secret.getId(), secret.getValue())
                .onSuccess(unused -> observable.invokeForEach(l -> l.created(secret)))
                .map(unused -> success(secret))
                .orElse(failure -> badRequest(failure.getFailureDetail()));
    }

    @Override
    public ServiceResult<Secret> delete(String secretKey) {
        var existing = findById(secretKey);
        if (existing == null) {
            return notFound("Secret " + secretKey + " not found");
        }

        return vault.deleteSecret(secretKey)
                .onSuccess(unused -> observable.invokeForEach(l -> l.deleted(existing)))
                .map(unused -> success(existing))
                .orElse(failure -> badRequest(failure.getFailureDetail()));
    }

    @Override
    public ServiceResult<Secret> update(Secret secret) {
        var existing = findById(secret.getId());
        if (existing == null) {
            return notFound("Secret " + secret.getId() + " not found");
        }

        return vault.storeSecret(secret.getId(), secret.getValue())
                .onSuccess(unused -> observable.invokeForEach(l -> l.updated(secret)))
                .map(unused -> success(secret))
                .orElse(failure -> badRequest(failure.getFailureDetail()));
    }

}

