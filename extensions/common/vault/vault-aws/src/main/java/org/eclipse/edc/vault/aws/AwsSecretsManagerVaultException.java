/*
 *  Copyright (c) 2022 - 2023 Amazon Web Services
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

public class AwsSecretsManagerVaultException extends RuntimeException {
    public AwsSecretsManagerVaultException(String format) {
        super(format);
    }
}
