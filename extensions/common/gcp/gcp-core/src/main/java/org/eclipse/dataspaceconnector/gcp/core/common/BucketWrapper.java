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

package org.eclipse.dataspaceconnector.gcp.core.common;

import com.google.cloud.storage.BucketInfo;

/**
 * Wrapper around  {@link BucketInfo} in order to decouple from GCP API.
 */
public class BucketWrapper {
    private final String name;

    public BucketWrapper(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
