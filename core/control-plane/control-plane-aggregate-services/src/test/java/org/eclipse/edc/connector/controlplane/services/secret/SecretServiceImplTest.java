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
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.secret.Secret;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecretServiceImplTest {

    private final Vault vault = mock();
    private final SecretObservable observable = mock();

    private final SecretService service = new SecretServiceImpl(vault, observable);


    @Test
    void createSecret_shouldCreateSecretIfItDoesNotAlreadyExist() {
        var secretId = "secretId";
        var secretValue = "secretValue";
        var secret = createSecret(secretId, secretValue);
        when(vault.storeSecret(secretId, secretValue)).thenReturn(Result.success());

        var inserted = service.create(secret);

        assertThat(inserted.succeeded()).isTrue();
        assertThat(inserted.getContent()).matches(hasId(secretId));
        verify(vault).storeSecret(secretId, secretValue);
        verify(observable).invokeForEach(any());
    }

    @Test
    void createSecret_shouldNotCreateSecretIfItAlreadyExists() {
        var secretId = "secretId";
        var secretValue = "secretValue";
        var secret = createSecret(secretId, secretValue);
        when(vault.resolveSecret(secretId)).thenReturn(secretValue);

        var inserted = service.create(secret);

        assertThat(inserted.failed()).isTrue();
        assertThat(inserted.getFailure().getReason()).isEqualTo(CONFLICT);
    }


    @Test
    void delete_shouldFailIfSecretDoesNotExist() {
        var secretId = "secretId";
        when(vault.deleteSecret(secretId)).thenReturn(Result.failure("secret not found"));

        var deleted = service.delete("serviceId");

        assertThat(deleted.failed()).isTrue();
        assertThat(deleted.getFailure().getReason()).isEqualTo(NOT_FOUND);
    }


    @Test
    void updateSecret_shouldUpdateWhenExists() {
        var secretId = "secretId";
        var oldSecretValue = "oldSecretValue";
        var secretValue = "secretValue";
        var secret = createSecret(secretId, secretValue);

        when(vault.resolveSecret(secretId)).thenReturn(oldSecretValue);
        when(vault.storeSecret(secretId, secretValue)).thenReturn(Result.success());

        var updated = service.update(secret);

        assertThat(updated.succeeded()).isTrue();
        verify(vault).storeSecret(eq(secretId), eq(secretValue));

        verify(observable).invokeForEach(any());
    }

    @Test
    void updateSecret_shouldReturnNotFound_whenNotExists() {
        var secretId = "secretId";
        var secretValue = "secretValue";
        var secret = createSecret(secretId, secretValue);

        when(vault.storeSecret(secretId, secretValue)).thenReturn(Result.failure("secret not found"));

        var updated = service.update(secret);

        assertThat(updated.failed()).isTrue();
        assertThat(updated.reason()).isEqualTo(NOT_FOUND);

        verify(observable, never()).invokeForEach(any());
    }


    @NotNull
    private Predicate<Secret> hasId(String secretId) {
        return it -> secretId.equals(it.getId());
    }

    private Secret createSecret(String secretId, String secretValue) {
        return createSecretBuilder(secretId, secretValue).build();
    }

    private Secret.Builder createSecretBuilder(String secretId, String secretValue) {
        return Secret.Builder.newInstance().id(secretId).value(secretValue);
    }
}
