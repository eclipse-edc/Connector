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

package org.eclipse.edc.api.auth.spi;

/**
 * Well-known scope strings for the Management API. The grammar is {@code management-api:[resource:]action}; the constants
 * below are the coarse, resource-wildcard tier (see the
 * <a href="https://github.com/eclipse-edc/Connector/tree/main/docs/developer/decision-records/2026-06-06-scope-based-api-authorization">decision record</a>).
 */
public interface ManagementApiScopes {

    /**
     * The namespace (scope prefix) for all Management API scopes.
     */
    String NAMESPACE = "management-api";

    /**
     * Read access to (any) resource: shorthand for {@code management-api:*:read}.
     */
    String READ = NAMESPACE + ":read";

    /**
     * Write access to (any) resource: shorthand for {@code management-api:*:write}. Implies {@link #READ}.
     */
    String WRITE = NAMESPACE + ":write";

    /**
     * Cross-tenant elevation / superuser access: shorthand for {@code management-api:*:admin}. Implies {@link #WRITE}
     * and {@link #READ}, and additionally bypasses the resource-ownership check.
     */
    String ADMIN = NAMESPACE + ":admin";
}
