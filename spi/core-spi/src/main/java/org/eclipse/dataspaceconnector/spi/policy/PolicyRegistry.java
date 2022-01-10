/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.policy;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.system.Feature;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * A registry of known policies.
 */
@Deprecated
@Feature(PolicyRegistry.FEATURE)
public interface PolicyRegistry {

    String FEATURE = "edc:policy:registry";

    /**
     * Resolves a policy for the given identifier.
     *
     * @param id the policy identifier.
     * @return the policy or null if not found.
     */
    @Nullable
    Policy resolvePolicy(String id);

    /**
     * Returns all available policies.
     */
    Collection<Policy> allPolicies();

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
