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

package org.eclipse.edc.validator.registration.spi;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Manages {@link SchemaValidatorRegistration} entries and wires the corresponding validators into the running
 * management API validation machinery.
 */
@ExtensionPoint
public interface SchemaValidatorRegistrationService {

    /**
     * Returns a registration by its id, or null if not found.
     */
    SchemaValidatorRegistration findById(String id);

    /**
     * Search registrations.
     */
    ServiceResult<List<SchemaValidatorRegistration>> search(QuerySpec query);

    /**
     * Creates a registration and activates the corresponding validator. The referenced {@code schema} must be
     * available (cached as a {@code JSON_SCHEMA} document); otherwise a bad request is returned.
     */
    @NotNull
    ServiceResult<SchemaValidatorRegistration> create(SchemaValidatorRegistration registration);

    /**
     * Updates a registration.
     */
    @NotNull
    ServiceResult<SchemaValidatorRegistration> update(SchemaValidatorRegistration registration);

    /**
     * Deletes a registration. The associated validator becomes inactive (it returns success while no registration
     * for its {@code version}/{@code validatedType} exists).
     */
    @NotNull
    ServiceResult<SchemaValidatorRegistration> deleteById(String id);

}
