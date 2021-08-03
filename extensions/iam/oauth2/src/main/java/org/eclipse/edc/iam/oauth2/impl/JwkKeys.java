/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.iam.oauth2.impl;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Models an identity provider JWK keys response.
 */
public class JwkKeys {
    private List<JwkKey> keys;

    @Nullable
    public List<JwkKey> getKeys() {
        return keys;
    }

    public void setKeys(List<JwkKey> keys) {
        this.keys = keys;
    }
}
