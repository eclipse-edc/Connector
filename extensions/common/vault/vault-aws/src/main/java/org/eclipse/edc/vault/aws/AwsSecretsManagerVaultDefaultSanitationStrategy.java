/*
 *  Copyright (c) 2023 - 2023 Amazon Web Services
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

public class AwsSecretsManagerVaultDefaultSanitationStrategy implements AwsSecretsManagerVaultSanitationStrategy {
    private final Monitor monitor;

    public AwsSecretsManagerVaultDefaultSanitationStrategy(Monitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Many-to-one mapping from all strings into set of strings that only contains valid AWS Secrets Manager key names.
     * The implementation replaces all illegal characters with '_' and attaches the hash code of the original string to
     * minimize the likelihood of key collisions.
     *
     * @param originalKey any key
     * @return Valid AWS Secrets Manager key
     */
    @Override
    public String sanitizeKey(String originalKey) {
        var key = originalKey;
        if (originalKey.length() > 500) {
            key = originalKey.substring(0, 500);
        }
        var sb = new StringBuilder();
        boolean replacedIllegalCharacters = false;
        for (int i = 0; i < key.length(); i++) {
            var c = key.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '/' && c != '_' && c != '+' && c != '.' && c != '@' && c != '-') {
                replacedIllegalCharacters = true;
                sb.append('-');
            } else {
                sb.append(c);
            }
        }
        var newKey = sb.append('_').append(originalKey.hashCode()).toString();
        if (replacedIllegalCharacters) {
            monitor.warning(String.format("AWS Secret Manager vault reduced length or replaced illegal characters " +
                    "in original key name: %s. New name is %s", originalKey, newKey));
        }
        return newKey;
    }
}