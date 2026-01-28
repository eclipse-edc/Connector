/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.policy.cel.service;

import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.List;

/**
 * Service for managing CEL policy expressions.
 */
public interface CelPolicyExpressionService {


    /**
     * Creates a new CEL expression.
     *
     * @param expression the expression to create
     * @return a service result indicating success or failure
     */
    ServiceResult<Void> create(CelExpression expression);

    /**
     * Finds a CEL expression by its ID.
     *
     * @param id the ID of the expression to find
     * @return a service result containing the found expression or an error
     */
    ServiceResult<CelExpression> findById(String id);

    /**
     * Updates an existing CEL expression.
     *
     * @param expression the expression to update
     * @return a service result indicating success or failure
     */
    ServiceResult<Void> update(CelExpression expression);

    /**
     * Deletes a CEL expression by its ID.
     *
     * @param id the ID of the expression to delete
     * @return a service result indicating success or failure
     */
    ServiceResult<Void> delete(String id);

    /**
     * Queries CEL expressions based on the provided query specification.
     *
     * @param querySpec the query specification
     * @return a service result containing the list of matching CEL expressions or an error
     */
    ServiceResult<List<CelExpression>> query(QuerySpec querySpec);
}
