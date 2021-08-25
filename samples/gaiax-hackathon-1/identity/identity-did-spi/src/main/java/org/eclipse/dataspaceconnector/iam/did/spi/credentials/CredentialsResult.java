/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.iam.did.spi.credentials;

import java.util.Collections;
import java.util.Map;

/**
 * Results of a credentials verification operation.
 */
public class CredentialsResult {
    private boolean success = true;
    private String error;
    private Map<String, String> validatedCredentials = Collections.emptyMap();

    public CredentialsResult(String error) {
        this.success = false;
        this.error = error;
    }

    public CredentialsResult(Map<String, String> validatedCredentials) {
        this.validatedCredentials = validatedCredentials;
    }

    public boolean success() {
        return success;
    }

    public String error() {
        return error;
    }

    public Map<String, String> getValidatedCredentials() {
        return validatedCredentials;
    }
}
