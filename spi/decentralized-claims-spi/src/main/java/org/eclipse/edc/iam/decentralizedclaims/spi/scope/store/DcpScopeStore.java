/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.iam.decentralizedclaims.spi.scope.store;

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.List;

/**
 * Store for DCP scopes.
 */
@ExtensionPoint
public interface DcpScopeStore {

    /**
     * Saves a DCP scope.
     *
     * @param scope the scope to save
     * @return a store result indicating success or failure
     */
    StoreResult<Void> save(DcpScope scope);

    /**
     * Deletes a DCP scope by its ID.
     *
     * @param scopeId the ID of the scope to delete
     * @return a store result indicating success or failure
     */
    StoreResult<Void> delete(String scopeId);

    /**
     * Queries DCP scopes based on the provided query specification.
     *
     * @param spec the query specification
     * @return a store result containing the list of matching scopes
     */
    StoreResult<List<DcpScope>> query(QuerySpec spec);
}
