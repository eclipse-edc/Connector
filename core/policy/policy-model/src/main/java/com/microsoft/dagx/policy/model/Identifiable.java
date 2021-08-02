/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.policy.model;

/**
 * A uniquely identifiable type.
 */
public abstract class Identifiable {
    protected String uid;

    /**
     * Returns the id.
     */
    public String getUid() {
        return uid;
    }
}
