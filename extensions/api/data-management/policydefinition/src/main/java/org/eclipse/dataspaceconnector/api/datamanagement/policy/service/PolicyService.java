/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *    ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.policy.service;

import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;

import java.util.Collection;

/**
 * The following interface is created for the implementation of the policy endpoint.
 */

public interface PolicyService {

    /**
     * Returns a policy by its id
     *
     * @param policyId id of the policy
     * @return the policy, null if it's not found
     */
    Policy findById(String policyId);

    /**
     * Query policies
     *
     * @param query request
     * @return the collection of policies that match the query
     */
    Collection<Policy> query(QuerySpec query);

    /**
     * Delete a policy
     *
     * @param policyId the id of the policy to be deleted
     * @return Policy deleted if the policy is deleted correctly, failure otherwise
     */
    ServiceResult<Policy> delete(String policyId);

    /**
     * Create an policy
     *
     * @param policy the policy
     */
    ServiceResult<Policy> create(Policy policy);
}
