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

import com.google.cloud.storage.BucketInfo;

/**
 * Wrapper around  {@link BucketInfo} in order to decouple from GCP API.
 */
public class GcsBucket {
    private final String name;

    public GcsBucket(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
