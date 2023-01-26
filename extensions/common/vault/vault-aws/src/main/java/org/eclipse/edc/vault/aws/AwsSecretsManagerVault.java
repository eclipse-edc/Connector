/*
 *  Copyright (c) 2022 Amazon Web Services
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amazon Web Services - initial implementation
 *
 */

package org.eclipse.edc.vault.aws;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

/**
 * Vault adapter for AWS Secrets Manager.
 */
public class AwsSecretsManagerVault implements Vault {

    private final SecretsManagerClient smClient;
    private final Monitor monitor;

    public AwsSecretsManagerVault(SecretsManagerClient smClient, Monitor monitor) {
        this.smClient = smClient;
        this.monitor = monitor;
    }

    /**
     * Retrieves a secret. Any string can be used as a key. Keys that do not comply with AWS Secrets Managers requirements
     * will be transformed.
     *
     * @param key the key of the secret
     * @return the secret value or null if secret could not be found
     */
    @Override
    public @Nullable String resolveSecret(String key) {
        var sanitizedKey = sanitizeKey(key);
        var request = GetSecretValueRequest.builder().secretId(sanitizedKey).build();
        try {
            monitor.debug(String.format("Resolving secret '%s' from AWS Secrets manager", sanitizedKey));
            return smClient.getSecretValue(request).secretString();
        } catch (ResourceNotFoundException e) {
            monitor.debug(String.format("Couldn't resolve secret with key %s", sanitizedKey), e);
        } catch (RuntimeException serviceException) {
            monitor.severe(serviceException.getMessage(), serviceException);
        }
        return null;
    }

    /**
     * Creates a new secret. Does not overwrite secrets.
     *
     * @param key   the secret key
     * @param value the serialized secret value
     * @return success or failure
     */
    @Override
    public Result<Void> storeSecret(String key, String value) {
        var sanitizedKey = sanitizeKey(key);
        var request = CreateSecretRequest.builder().name(sanitizedKey)
                .secretString(value).build();
        try {
            monitor.debug(String.format("Storing secret '%s' to AWS Secrets manager", sanitizedKey));
            smClient.createSecret(request);
            return Result.success();
        } catch (RuntimeException serviceException) {
            monitor.severe(serviceException.getMessage(), serviceException);
            return Result.failure(serviceException.getMessage());
        }
    }

    /**
     * Deletes a secret without the possibility of recovery.
     *
     * @param key the secret's key
     * @return success or failure
     */
    @Override
    public Result<Void> deleteSecret(String key) {
        var sanitizedKey = sanitizeKey(key);
        var request = DeleteSecretRequest.builder().secretId(sanitizedKey)
                .forceDeleteWithoutRecovery(true).build();
        try {
            monitor.debug(String.format("Deleting secret '%s' from AWS Secrets manager", sanitizedKey));
            smClient.deleteSecret(request);
            return Result.success();
        } catch (RuntimeException serviceException) {
            monitor.severe(serviceException.getMessage(), serviceException);
            return Result.failure(serviceException.getMessage());
        }
    }

    /**
     * Many-to-one mapping from all strings into set of strings that only contains valid AWS Secrets Manager key names.
     * The implementation replaces all illegal characters with '-' and attaches the hash code of the original string to
     * minimize the likelihood of key collisions.
     *
     * @param originalKey any key
     * @return Valid AWS Secrets Manager key
     */
    String sanitizeKey(String originalKey) {
        var key = originalKey;
        if (originalKey.length() > 500) {
            key = originalKey.substring(0, 500);
        }
        StringBuilder sb = new StringBuilder();
        boolean replacedIllegalCharacters = false;
        for (int i = 0; i < key.length(); i++) {
            Character c = key.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '/' && c != '_' && c != '+' && c != '.' && c != '@' && c != '-') {
                replacedIllegalCharacters = true;
                sb.append('-');
            } else {
                sb.append(c);
            }
        }
        String newKey = sb.append('_').append(originalKey.hashCode()).toString();
        if (replacedIllegalCharacters) {
            monitor.warning(String.format("AWS Secret Manager vault reduced length or replaced illegal characters " +
                    "in original key name: %s. New name is %s", originalKey, newKey));
        }
        return newKey;
    }

}
