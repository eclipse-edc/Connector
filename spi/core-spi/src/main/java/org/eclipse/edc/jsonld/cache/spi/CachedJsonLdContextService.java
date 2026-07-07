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

package org.eclipse.edc.jsonld.cache.spi;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Manages {@link CachedJsonLdContext} entries and keeps the {@link org.eclipse.edc.jsonld.spi.JsonLd} service
 * in sync with the persisted state.
 */
@ExtensionPoint
public interface CachedJsonLdContextService {

    /**
     * Returns a cached context by its id, or null if not found.
     */
    CachedJsonLdContext findById(String id);

    /**
     * Search cached contexts.
     */
    ServiceResult<List<CachedJsonLdContext>> search(QuerySpec query);

    /**
     * Creates a cached context. The content is obtained according to the entry's {@code pullStrategy}
     * (supplied inline or fetched from its url). On success the document is registered into the {@code JsonLd} service.
     */
    @NotNull
    ServiceResult<CachedJsonLdContext> create(CachedJsonLdContext context);

    /**
     * Updates a cached context and re-registers it into the {@code JsonLd} service.
     */
    @NotNull
    ServiceResult<CachedJsonLdContext> update(CachedJsonLdContext context);

    /**
     * Deletes a cached context and unregisters it from the {@code JsonLd} service.
     */
    @NotNull
    ServiceResult<CachedJsonLdContext> deleteById(String id);

    /**
     * Forces a refresh of the given cached context by re-fetching it from its url (a no-op for pull strategy
     * {@code never}), then re-registers it into the {@code JsonLd} service.
     */
    @NotNull
    ServiceResult<CachedJsonLdContext> refresh(String id);

}
