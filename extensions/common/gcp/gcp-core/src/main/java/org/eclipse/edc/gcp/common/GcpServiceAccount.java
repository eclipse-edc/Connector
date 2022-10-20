/*
 *  Copyright (c) 2022 Google LLC
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Google LCC - Initial implementation
 *
 */

package org.eclipse.edc.gcp.common;

import com.google.iam.admin.v1.ServiceAccount;

/**
 * Wrapper around  {@link ServiceAccount} in order to decouple from GCP API.
 */
public class GcpServiceAccount {
    private final String email;
    private final String name;
    private final String description;

    public GcpServiceAccount(String email, String name, String description) {
        this.email = email;
        this.name = name;
        this.description = description;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}