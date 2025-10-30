/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.store.sql.participantcontext.schema.postgres;

import org.eclipse.edc.connector.store.sql.participantcontext.ParticipantContextStoreStatements;
import org.eclipse.edc.sql.translation.TranslationMapping;


/**
 * Provides a mapping from the canonical format to SQL column names for a {@code VerifiableCredentialResource}
 */
public class ParticipantContextMapping extends TranslationMapping {

    public static final String FIELD_ID = "participantContextId";
    public static final String FIELD_CREATE_TIMESTAMP = "createdAt";
    public static final String FIELD_LASTMODIFIED_TIMESTAMP = "lastModified";
    public static final String FIELD_STATE = "state";

    public ParticipantContextMapping(ParticipantContextStoreStatements statements) {
        add(FIELD_ID, statements.getIdColumn());
        add(FIELD_CREATE_TIMESTAMP, statements.getCreateTimestampColumn());
        add(FIELD_STATE, statements.getStateColumn());
        add(FIELD_LASTMODIFIED_TIMESTAMP, statements.getLastModifiedTimestampColumn());
    }
}