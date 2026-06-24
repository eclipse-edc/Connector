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

package org.eclipse.edc.policy.cel.store;

import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.List;

/**
 * Store interface for managing CEL (Common Expression Language) expressions.
 */
public interface CelExpressionStore {

    /**
     * Creates a new CEL expression to the store.
     *
     * @param expression the CEL expression to save
     * @return a store result indicating success or failure
     */
    StoreResult<Void> create(CelExpression expression);

    /**
     * Updates an existing CEL expression in the store.
     *
     * @param expression the CEL expression to update
     * @return a store result indicating success or failure
     */
    StoreResult<Void> update(CelExpression expression);

    /**
     * Deletes a CEL expression from the store by its ID.
     *
     * @param id the ID of the CEL expression to delete
     * @return a store result indicating success or failure
     */
    StoreResult<Void> delete(String id);

    /**
     * Queries CEL expressions based on the provided query specification.
     *
     * @param querySpec the query specification
     * @return a list of matching CEL expressions
     */
    List<CelExpression> query(QuerySpec querySpec);

    default String alreadyExistsErrorMessage(String id) {
        return "A CelExpression with ID '%s' already exists.".formatted(id);
    }

    default String notFoundErrorMessage(String id) {
        return "A CelExpression with ID '%s' does not exist.".formatted(id);
    }
}
