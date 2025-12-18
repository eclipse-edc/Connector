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

import org.eclipse.edc.encryption.EncryptionAlgorithm;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesEncryptionAlgorithm implements EncryptionAlgorithm {
    public static final int GCM_AUTH_TAG_LENGTH = 128;
    public static final String AES = "AES";
    public static final int IV_SIZE_BYTES = 12; // 12 bytes
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int[] ALLOWED_SIZES = new int[]{16, 24, 32}; // AES allows for 128, 192 or 256 bits
    private final Vault vault;
    private final String aesKeyAlias;
    private final Base64.Decoder decoder = Base64.getDecoder();
    private final Base64.Encoder encoder = Base64.getEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public AesEncryptionAlgorithm(Vault vault, String aesKeyAlias) {
        this.vault = vault;
        this.aesKeyAlias = aesKeyAlias;
    }

    @Override
    public Result<String> encrypt(String plainText) {
        return getKey(aesKeyAlias)
                .compose(key -> doEncrypt(plainText, key));

    }

    @Override
    public Result<String> decrypt(String cipherText) {
        return getKey(aesKeyAlias)
                .compose(key -> doDecrypt(cipherText, key));
    }

    private Result<String> doEncrypt(String plainText, SecretKey key) {
        try {
            var iv = generateIv(IV_SIZE_BYTES);
            var gcmSpec = new GCMParameterSpec(GCM_AUTH_TAG_LENGTH, iv);
            var cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes());
            byte[] combinedIvAndCipherText = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combinedIvAndCipherText, 0, iv.length);
            System.arraycopy(cipherText, 0, combinedIvAndCipherText, iv.length, cipherText.length);
            return Result.success(encoder.encodeToString(combinedIvAndCipherText));
        } catch (Exception e) {
            return Result.failure("Error while encrypting: %s".formatted(e.getMessage()));
        }
    }

    private Result<String> doDecrypt(String cipherText, SecretKey key) {
        try {
            var decodedCipher = decoder.decode(cipherText);

            if (decodedCipher.length < IV_SIZE_BYTES) {
                return Result.failure("Decoded ciphertext was shorter than the IV size (%d)".formatted(IV_SIZE_BYTES));
            }
            var iv = Arrays.copyOfRange(decodedCipher, 0, IV_SIZE_BYTES);
            var actualCipherText = Arrays.copyOfRange(decodedCipher, IV_SIZE_BYTES, decodedCipher.length);

            var gcmSpec = new GCMParameterSpec(GCM_AUTH_TAG_LENGTH, iv);
            var cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
            byte[] plainText = cipher.doFinal(actualCipherText);
            return Result.success(new String(plainText));
        } catch (Exception e) {
            return Result.failure("Error while decrypting: %s".formatted(e.getMessage()));
        }
    }

    private byte[] generateIv(int length) {
        byte[] iv = new byte[length];
        secureRandom.nextBytes(iv);
        return iv;
    }


    private Result<SecretKey> getKey(String alias) {
        var secretBase64 = vault.resolveSecret(alias);
        if (secretBase64 == null) {
            return Result.failure("Cannot perform AES encryption: secret key not found in vault");
        }
        try {
            var decoded = decoder.decode(secretBase64);
            if (isAllowedSize(decoded)) {
                return Result.success(new SecretKeySpec(decoded, AES));
            } else {
                return Result.failure("Expected a key size of 16, 24 or 32 bytes byt found " + decoded.length);
            }
        } catch (Exception e) {
            return Result.failure("Error while decoding the secret key from base64: %s".formatted(e.getMessage()));
        }

    }

    /**
     * Check is the decoded byte array is 16, 24 or 32 bytes in size
     */
    private boolean isAllowedSize(byte[] decoded) {
        return Arrays.stream(ALLOWED_SIZES).anyMatch(i -> i == decoded.length);
    }


}
