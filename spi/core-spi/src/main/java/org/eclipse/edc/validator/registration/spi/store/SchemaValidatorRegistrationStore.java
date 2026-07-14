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

package org.eclipse.edc.validator.registration.spi.store;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration;

import java.util.List;
import java.util.stream.Stream;

/**
 * Persists {@link SchemaValidatorRegistration} entries. Implementations must be thread-safe.
 */
@ExtensionPoint
public interface SchemaValidatorRegistrationStore {

    String NOT_FOUND = "Schema validator registration with ID %s could not be found";
    String ALREADY_EXISTS = "Schema validator registration with ID %s already exists";

    /**
     * Finds a registration by its id.
     *
     * @param id the id.
     * @return the entry or null if not found.
     * @throws EdcPersistenceException if something goes wrong.
     */
    SchemaValidatorRegistration findById(String id);

    /**
     * Returns all registrations for the given management API version and validated JSON-LD @type.
     *
     * @param version       the management API version (e.g. {@code v5}).
     * @param validatedType the JSON-LD @type term the schema is bound to.
     * @return a list, might be empty, never null.
     * @throws EdcPersistenceException if something goes wrong.
     */
    List<SchemaValidatorRegistration> findByVersionAndValidatedType(String version, String validatedType);

    /**
     * Returns a stream of registrations matching the query spec.
     *
     * @param spec the query spec.
     * @return a stream, might be empty, never null.
     * @throws EdcPersistenceException if something goes wrong.
     */
    Stream<SchemaValidatorRegistration> findAll(QuerySpec spec);

    /**
     * Persists the entry if it does not yet exist.
     *
     * @param registration the entry to store.
     * @return {@link StoreResult#success} or {@link StoreResult#alreadyExists} if an entry with the same id exists.
     */
    StoreResult<SchemaValidatorRegistration> create(SchemaValidatorRegistration registration);

    /**
     * Updates an existing entry.
     *
     * @param registration the entry to update.
     * @return {@link StoreResult#success} or {@link StoreResult#notFound} if no entry with the same id exists.
     */
    StoreResult<SchemaValidatorRegistration> update(SchemaValidatorRegistration registration);

    /**
     * Deletes the entry with the given id.
     *
     * @param id the id of the entry to delete.
     * @return {@link StoreResult#success} with the deleted entry, or {@link StoreResult#notFound} if not found.
     */
    StoreResult<SchemaValidatorRegistration> delete(String id);

}
