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

package org.eclipse.edc.validator.registration.store.sql.schema.postgres;

import org.eclipse.edc.sql.translation.TranslationMapping;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration;
import org.eclipse.edc.validator.registration.store.sql.schema.SchemaValidatorRegistrationStoreStatements;

/**
 * Maps fields of a {@link SchemaValidatorRegistration} onto the corresponding SQL columns.
 */
public class SchemaValidatorRegistrationMapping extends TranslationMapping {

    public SchemaValidatorRegistrationMapping(SchemaValidatorRegistrationStoreStatements statements) {
        add("id", statements.getIdColumn());
        add("version", statements.getVersionColumn());
        add("validatedType", statements.getValidatedTypeColumn());
        add("schema", statements.getSchemaColumn());
        add("profiles", statements.getProfilesColumn());
        add("createdAt", statements.getCreatedAtColumn());
        add("updatedAt", statements.getUpdatedAtColumn());
    }
}
