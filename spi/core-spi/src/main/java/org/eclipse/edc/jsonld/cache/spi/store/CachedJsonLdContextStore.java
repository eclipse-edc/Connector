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

package org.eclipse.edc.jsonld.cache.spi.store;

import org.eclipse.edc.jsonld.cache.spi.CachedJsonLdContext;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.stream.Stream;

/**
 * Persists {@link CachedJsonLdContext} entries. Implementations must be thread-safe.
 */
@ExtensionPoint
public interface CachedJsonLdContextStore {

    String NOT_FOUND = "Cached JSON-LD context with ID %s could not be found";
    String ALREADY_EXISTS = "Cached JSON-LD context with ID %s already exists";
    String URL_ALREADY_EXISTS = "Cached JSON-LD context with URL %s already exists";

    /**
     * Finds a cached context by its id.
     *
     * @param id the id.
     * @return the entry or null if not found.
     * @throws EdcPersistenceException if something goes wrong.
     */
    CachedJsonLdContext findById(String id);

    /**
     * Finds a cached context by its url.
     *
     * @param url the context url.
     * @return the entry or null if not found.
     * @throws EdcPersistenceException if something goes wrong.
     */
    CachedJsonLdContext findByUrl(String url);

    /**
     * Returns a stream of cached contexts matching the query spec.
     *
     * @param spec the query spec.
     * @return a stream, might be empty, never null.
     * @throws EdcPersistenceException if something goes wrong.
     */
    Stream<CachedJsonLdContext> findAll(QuerySpec spec);

    /**
     * Persists the entry if it does not yet exist.
     *
     * @param context the entry to store.
     * @return {@link StoreResult#success} or {@link StoreResult#alreadyExists} if an entry with the same id or url exists.
     */
    StoreResult<CachedJsonLdContext> create(CachedJsonLdContext context);

    /**
     * Updates an existing entry.
     *
     * @param context the entry to update.
     * @return {@link StoreResult#success} or {@link StoreResult#notFound} if no entry with the same id exists.
     */
    StoreResult<CachedJsonLdContext> update(CachedJsonLdContext context);

    /**
     * Deletes the entry with the given id.
     *
     * @param id the id of the entry to delete.
     * @return {@link StoreResult#success} with the deleted entry, or {@link StoreResult#notFound} if not found.
     */
    StoreResult<CachedJsonLdContext> delete(String id);

}
