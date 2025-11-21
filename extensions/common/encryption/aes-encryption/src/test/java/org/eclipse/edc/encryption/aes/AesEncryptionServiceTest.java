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

package org.eclipse.edc.encryption.aes;

import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.encryption.aes.AesEncryptionService.IV_SIZE_BYTES;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class AesEncryptionServiceTest {

    private static final String TEST_ALIAS = "test-alias";
    private final Vault vaultMock = mock();
    private AesEncryptionService encryptionService;

    @BeforeEach
    void setup() {
        encryptionService = new AesEncryptionService(vaultMock, TEST_ALIAS);
    }


    @ParameterizedTest
    @ValueSource(ints = {16, 24, 32})
    void encrypt_and_decrypt_roundTrip_for_allowed_key_sizes(int keySize) {
        when(vaultMock.resolveSecret(eq(TEST_ALIAS))).thenReturn(generateBase64(keySize));

        var raw = "The quick brown fox jumps over the lazy dog";
        var encResult = encryptionService.encrypt(raw);

        // result-level assertions
        assertThat(encResult).isSucceeded();
        var cipherText = encResult.getContent();
        assertThat(cipherText).isNotNull();

        // verify returned ciphertext is valid base64 and contains IV of expected size
        var decoded = Base64.getDecoder().decode(cipherText);
        assertThat(decoded.length).isGreaterThanOrEqualTo(IV_SIZE_BYTES + 1); // at least IV + 1 byte of ciphertext

        // verify decrypt returns original text
        var decResult = encryptionService.decrypt(cipherText);
        assertThat(decResult).isSucceeded();
        assertThat(decResult.getContent()).isEqualTo(raw);
    }

    @ParameterizedTest
    @ValueSource(ints = {16, 24, 32})
    void encrypt_different_output(int keySize) {
        when(vaultMock.resolveSecret(eq(TEST_ALIAS))).thenReturn(generateBase64(keySize));

        var raw = "The quick brown fox jumps over the lazy dog";
        var first = encryptionService.encrypt(raw);
        var second = encryptionService.encrypt(raw);

        // result-level assertions
        assertThat(first).isSucceeded();
        assertThat(second).isSucceeded();

        assertThat(first.getContent()).isNotEqualTo(second.getContent());

    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 17, 33, 1024, 8192})
    void encrypt_invalidKeySize(int invalidKeySize) {
        when(vaultMock.resolveSecret(eq(TEST_ALIAS))).thenReturn(generateBase64(invalidKeySize));
        assertThat(encryptionService.encrypt("hello world!")).isFailed()
                .detail()
                .contains("Expected a key size of 16, 24 or 32 bytes byt found " + invalidKeySize);
    }

    @Test
    void encrypt_secretNotBase64() {
        when(vaultMock.resolveSecret(eq(TEST_ALIAS))).thenReturn("not-base64");

        assertThat(encryptionService.encrypt("hello world!")).isFailed()
                .detail()
                .contains("Illegal base64 character");
    }

    @Test
    void encrypt_secretNotExist() {
        assertThat(encryptionService.encrypt("hello world!")).isFailed()
                .detail()
                .contains("Cannot perform AES encryption: secret key not found in vault");
    }

    @Test
    void decrypt_wrongSecretKey() {
        when(vaultMock.resolveSecret(eq(TEST_ALIAS))).thenReturn(generateBase64(32));
        var rawText = "hello world!";
        var encrypted = encryptionService.encrypt(rawText).getContent();
        when(vaultMock.resolveSecret(eq(TEST_ALIAS))).thenReturn(generateBase64(32));
        assertThat(encryptionService.decrypt(encrypted)).isFailed()
                .detail()
                .contains("Error while decrypting");
    }

    @Test
    void decrypt_keyNotBase64() {
        when(vaultMock.resolveSecret(eq(TEST_ALIAS))).thenReturn("not-base64");
        assertThat(encryptionService.decrypt("encrypted-text")).isFailed()
                .detail()
                .contains("Illegal base64 character");
    }

    @Test
    void decrypt_ciphertextTooShort() {
        when(vaultMock.resolveSecret(eq(TEST_ALIAS))).thenReturn(generateBase64(32));
        var cipherText = Base64.getEncoder().encodeToString("asdf".getBytes());
        assertThat(encryptionService.decrypt(cipherText)).isFailed()
                .detail()
                .contains("Decoded ciphertext was shorter than the IV size (12)");
    }

    private String generateBase64(int validSize) {
        var bytes = new byte[validSize];

        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);

    }
}
