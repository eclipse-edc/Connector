/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.verifiablecredentials;

import org.eclipse.edc.iam.verifiablecredentials.revocation.RevocationServiceRegistryImpl;
import org.eclipse.edc.iam.verifiablecredentials.spi.RevocationListService;
import org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RevocationServiceRegistryImplTest {

    private final RevocationServiceRegistryImpl registry = new RevocationServiceRegistryImpl(mock());

    @Test
    void checkValidity() {
        var mockService = mock(RevocationListService.class);
        registry.addService("test-type", mockService);
        when(mockService.checkValidity(any(CredentialStatus.class))).thenReturn(Result.success());

        var cred = TestFunctions.createCredentialBuilder().credentialStatus(new CredentialStatus("test-id", "test-type", Map.of())).build();
        assertThat(registry.checkValidity(cred)).isSucceeded();
    }

    @Test
    void checkValidity_whenNoCredentialStatus() {
        var mockService = mock(RevocationListService.class);
        registry.addService("test-type", mockService);
        when(mockService.checkValidity(any(CredentialStatus.class))).thenReturn(Result.success());

        var cred = TestFunctions.createCredentialBuilder().build();
        assertThat(registry.checkValidity(cred)).isSucceeded();
    }

    @Test
    void checkValidity_noServiceFound_shouldReturnSuccess() {
        var mockService = mock(RevocationListService.class);
        registry.addService("test-type", mockService);
        when(mockService.checkValidity(any(CredentialStatus.class))).thenReturn(Result.success());

        var cred = TestFunctions.createCredentialBuilder().credentialStatus(new CredentialStatus("test-id", "not-exist", Map.of())).build();
        assertThat(registry.checkValidity(cred)).isSucceeded();
    }

    @Test
    void checkValidity_oneInvalid_shouldReturnFailure() {
        var mockService = mock(RevocationListService.class);
        registry.addService("test-type", mockService);
        when(mockService.checkValidity(any(CredentialStatus.class)))
                .thenReturn(Result.success())
                .thenReturn(Result.failure("test failure"));

        var cred = TestFunctions.createCredentialBuilder()
                .credentialStatus(new CredentialStatus("test-id", "test-type", Map.of()))
                .credentialStatus(new CredentialStatus("test-id", "test-type", Map.of()))
                .build();
        assertThat(registry.checkValidity(cred)).isFailed()
                .detail().isEqualTo("test failure");
    }

    @Test
    void checkValidity_allInvalid_shouldReturnFailure() {
        var mockService = mock(RevocationListService.class);
        registry.addService("test-type", mockService);
        when(mockService.checkValidity(any(CredentialStatus.class)))
                .thenReturn(Result.failure("test failure"));

        var cred = TestFunctions.createCredentialBuilder()
                .credentialStatus(new CredentialStatus("test-id", "test-type", Map.of()))
                .credentialStatus(new CredentialStatus("test-id", "test-type", Map.of()))
                .build();
        assertThat(registry.checkValidity(cred)).isFailed()
                .detail().contains("test failure");
    }
}