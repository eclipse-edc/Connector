/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.policy;

import com.microsoft.dagx.policy.model.Policy;
import org.jetbrains.annotations.Nullable;

/**
 * A registry of known policies.
 */
public interface PolicyRegistry {

    /**
     * Resolves a policy for the given identifier.
     *
     * @param id the policy identifier.
     * @return the policy or null if not found.
     */
    @Nullable
    Policy resolvePolicy(String id);

    /**
     * Registers the policy in the system.
     *
     * @param policy the policy
     */
    void registerPolicy(Policy policy);

    /**
     * Removes a policy with the given id.
     */
    void removePolicy(String id);
}
