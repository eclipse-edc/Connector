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

/**
 * Interface for key sanitation strategies.
 */
public interface AwsSecretsManagerVaultSanitationStrategy {

    /**
     * Maps any string to a valid AWS Secrets Manager key.
     *
     * @param originalKey any key
     * @return Valid AWS Secrets Manager key
     */
    String sanitizeKey(String originalKey);
}
