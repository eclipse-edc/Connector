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

package org.eclipse.edc.iam.decentralizedclaims.spi.scope;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.List;

/**
 * Registry for DCP scopes.
 */
@ExtensionPoint
public interface DcpScopeRegistry {

    /**
     * Registers a DCP scope.
     *
     * @param scope the scope to register
     * @return a service result indicating success or failure
     */
    ServiceResult<Void> register(DcpScope scope);

    /**
     * Removes a DCP scope by its ID.
     *
     * @param scopeId the ID of the scope to remove
     * @return a service result indicating success or failure
     */
    ServiceResult<Void> remove(String scopeId);

    /**
     * Retrieves the default DCP scopes.
     *
     * @return a service result containing the list of default scopes
     */
    ServiceResult<List<DcpScope>> getDefaultScopes();

    /**
     * Retrieves all scope mappings.
     *
     * @return a service result containing the list of all scope mappings
     */
    ServiceResult<List<DcpScope>> getScopeMapping();

}
