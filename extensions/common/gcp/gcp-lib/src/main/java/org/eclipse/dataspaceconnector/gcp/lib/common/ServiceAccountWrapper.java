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

package org.eclipse.dataspaceconnector.gcp.lib.common;

import com.google.iam.admin.v1.ServiceAccount;

/**
 * Wrapper around  {@link ServiceAccount} in order to decouple from GCP API.
 */
public class ServiceAccountWrapper {
    private final String email;
    private final String name;

    public ServiceAccountWrapper(ServiceAccount serviceAccount) {
        email = serviceAccount.getEmail();
        name = serviceAccount.getName();
    }

    public ServiceAccountWrapper(String email, String name) {
        this.email = email;
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }
}