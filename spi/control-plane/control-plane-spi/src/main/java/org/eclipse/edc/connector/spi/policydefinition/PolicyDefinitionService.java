/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.spi.policydefinition;

import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * The following interface is created for the implementation of the policy definition endpoint.
 */
@ExtensionPoint
public interface PolicyDefinitionService {

    /**
     * Returns a policy by its id
     *
     * @param policyId id of the policy
     * @return the policy, null if it's not found
     */

    PolicyDefinition findById(String policyId);

    /**
     * Query policies
     *
     * @param query request
     * @return the stream of policies that match the query
     */

    ServiceResult<Stream<PolicyDefinition>> query(QuerySpec query);

    /**
     * Delete a policy
     *
     * @param policyId the id of the policy to be deleted
     * @return Policy deleted if the policy is deleted correctly, failure otherwise
     */

    @NotNull
    ServiceResult<PolicyDefinition> deleteById(String policyId);

    /**
     * Create an policy
     *
     * @param policy the policy
     */

    @NotNull
    ServiceResult<PolicyDefinition> create(PolicyDefinition policy);

    /**
     * Updates a policy. If the policy does not yet exist, {@link ServiceResult#notFound(String)} will be returned.
     *
     * @param policy the contents of the policy.
     * @return successful if updated, a failure otherwise.
     */
    ServiceResult<PolicyDefinition> update(PolicyDefinition policy);
}
