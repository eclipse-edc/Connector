package com.microsoft.dagx.policy.engine.model;

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
